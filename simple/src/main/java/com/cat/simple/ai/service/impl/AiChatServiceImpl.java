package com.cat.simple.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.DTO;
import com.cat.common.entity.HttpResult;
import com.cat.common.entity.ai.chat.ChatMessage;
import com.cat.common.entity.ai.chat.ChatRequestParam;
import com.cat.common.entity.ai.chat.ChatSession;
import com.cat.common.entity.ai.chat.QAMessage;
import com.cat.common.entity.ai.model.AiModel;
import com.cat.common.entity.ai.model.ModelType;
import com.cat.common.entity.auth.LoginUser;
import com.cat.common.entity.file.FileInfo;
import com.cat.common.utils.UUIDUtils;
import com.cat.simple.ai.mapper.ChatMessageMapper;
import com.cat.simple.ai.mapper.ChatSessionMapper;
import com.cat.simple.ai.service.AiChatService;
import com.cat.simple.ai.service.AiModelService;
import com.cat.simple.ai.service.LlmService;
import com.cat.simple.config.rocketmq.post.qa.QAVectorRockerMqProductor;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.file.service.FileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    @Resource
    private AiModelService aiModelService;

    @Resource
    private LlmService llmService;

    @Resource
    private ChatSessionMapper chatSessionMapper;

    @Resource
    private ChatMessageMapper chatMessageMapper;

    // ✅ OPT: 注入编程式事务模板，替代流式场景下不可用的 @Transactional
    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private FileService fileService;

    @Resource
    private QAVectorRockerMqProductor qaVectorRockerMqProductor;

    // ✅ OPT: 移除未使用的 okhttp3.* 和 ThreadPoolTaskExecutor 导入/注入

    @Override
    // ✅ FIX: 移除 @Transactional。该方法包含异步流式分支，声明式事务会导致连接泄漏和数据不一致
    public Object chat(ChatRequestParam chatRequestParam) {

        LoginUser loginUser = SecurityUtils.getLoginUser();
        String userId = Objects.requireNonNull(loginUser).getUserId();
        LocalDateTime now = LocalDateTime.now();
        ChatSession chatSession;
        List<ChatMessage> chatMessages = new ArrayList<>();
        List<FileInfo> fileInfos = new ArrayList<>();
        if(!CollectionUtils.isEmpty(chatRequestParam.getFileIds())){
            for (String fileId : chatRequestParam.getFileIds()) {
                fileInfos.add(fileService.getAgentFileInfoById(fileId));
            }
        }

        ChatMessage askMessage = new ChatMessage()
                .setMessageId(UUIDUtils.randomUUID())
                .setCreateTime(now)
                .setContent(chatRequestParam.getContent())
                .setFiles(fileInfos)
                .setRole(MessageType.USER.getValue());

        ChatMessage ansMessage = new ChatMessage()
                .setMessageId(UUIDUtils.randomUUID());

        // 1. 处理会话与历史消息
        if (StringUtils.hasText(chatRequestParam.getSessionId())) {
            chatSession = chatSessionMapper.selectOne(new LambdaQueryWrapper<ChatSession>()
                    .eq(ChatSession::getSessionId, chatRequestParam.getSessionId())
                    .eq(ChatSession::getUserId, userId)
            );
            if (Objects.isNull(chatSession)) {
                throw new IllegalStateException("sessionId is error");
            }
            chatSession.setUpdateTime(now);
            // ✅ FIX: 上一版已修复为 getSessionId，此处保留；增加排序保证上下文顺序
            chatMessages = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getSessionId, chatSession.getSessionId())
                    .orderByAsc(ChatMessage::getCreateTime)
            );
        } else {
            // ✅ OPT: 用用户首条消息截断作为默认标题，避免列表空白 + 作为"待AI生成"标记
            String defaultTitle = chatRequestParam.getContent();
            if (StringUtils.hasText(defaultTitle)) {
                defaultTitle = defaultTitle.trim();
                if (defaultTitle.length() > 15) {
                    defaultTitle = defaultTitle.substring(0, 15) + "...";
                }
            } else {
                defaultTitle = "新对话";
            }

            chatSession = new ChatSession()
                    .setSessionId(UUIDUtils.randomUUID())
                    .setUserId(userId)
                    .setCreateTime(now)
                    .setUpdateTime(now)
                    .setTitle(defaultTitle);

            chatSessionMapper.insert(chatSession);
        }

        askMessage.setSessionId(chatSession.getSessionId());
        ansMessage.setSessionId(chatSession.getSessionId());

        chatMessageMapper.insert(askMessage);
        chatMessages.add(askMessage);

        // 2. 构建大模型请求
        AiModel aiModel = aiModelService.getOneWithRealApiKeyById(chatRequestParam.getModelId());
        OpenAiChatModel openAiChatModel = llmService.buildOpenAiChatModel(aiModel);
        Prompt prompt = buildPrompt(chatMessages, aiModel.getVision());

//        OpenAiChatModel openAiChatModel = llmService.buildOpenAiChatModel(aiModel);


        // 3. 分流处理
        if (chatRequestParam.isStream()) {

            return handleStreamChat(openAiChatModel, prompt, chatSession, askMessage, ansMessage);

        } else {

//            return HttpResult.back(handleStreamChatBackChatMessage(openAiChatModel, prompt, chatSession, askMessage, ansMessage));
            return HttpResult.back(handleSyncChat(openAiChatModel, prompt, chatSession, askMessage, ansMessage));
        }
    }

    @Override
    public List<ChatSession> sessions() {
        return chatSessionMapper.selectList(new LambdaQueryWrapper<ChatSession>().eq(ChatSession::getUserId, Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId()).orderByDesc(ChatSession::getUpdateTime));
    }

    @Override
    public List<ChatMessage> messages(String sessionId) {
        ChatSession session = chatSessionMapper.selectOne(new LambdaQueryWrapper<ChatSession>().eq(ChatSession::getSessionId, sessionId).eq(ChatSession::getUserId, Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId()));
        if(Objects.isNull(session)){
            throw new IllegalStateException("会话id无效");
        }


        return chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getSessionId, sessionId).orderByAsc(ChatMessage::getCreateTime));
    }

    @Override
    public List<AiModel> chatModels() {
        return aiModelService.list(ModelType.CHAT);
    }

    @Override
    public FileInfo fileUpload(MultipartFile file) throws IOException {
        DTO<FileInfo> fileInfoDTO = fileService.uploadAgentFile(file);
        return fileInfoDTO.getData();
    }

    @Override
    public void fileDownload(String fileId) throws IOException {
        fileService.downloadAgentFile(fileId);
    }




    private Prompt buildPrompt(List<ChatMessage> chatMessages, boolean vision){
        List<Message> messages = new ArrayList<>();
        String username = Objects.requireNonNull(SecurityUtils.getLoginUser()).getUsername();
        String importPrompt = """
        - 当前用户账号：%s
        """.formatted(username);
        messages.add(SystemMessage.builder().text("""
        你是一个专业的AI助手，请严格遵循以下角色设定进行对话：

        ## 角色身份
        - 名称：希斯莱杰
        - 身份：小丑
        - 性格特征：搞怪，癫狂，说一些奇怪的话

        ## 行为规范
        1. 始终以第一人称"希斯莱杰"的口吻回复，不要暴露AI身份
        2. 保持角色一致性，不跳出人设，不使用"作为AI"等表述
        3. 回复风格应符合角色性格：搞怪，癫狂，说一些奇怪的话
        4. 对超出角色知识范围的问题，用符合人设的方式委婉回应，而非直接拒绝

        ## 对话约束
        - 语言：中文
        - 禁止输出任何与角色无关的元信息、解释或免责声明
        """).build());
        for (ChatMessage chatMessage : chatMessages) {
            MessageType messageType = switch (chatMessage.getRole()){
                case "user" -> MessageType.USER;
                case "assistant" -> MessageType.ASSISTANT;
                case "system" -> MessageType.SYSTEM;
                default -> throw new IllegalArgumentException(
                        "Unsupported message type: " + chatMessage.getRole());
            };

            List<Media> mediaList = new ArrayList<>();
            if(vision && !CollectionUtils.isEmpty(chatMessage.getFiles())){

                for (FileInfo fileInfo : chatMessage.getFiles()) {

                    MimeType mimeType = MimeType.valueOf(fileInfo.getContentType());

                    List<MimeType> allowMimeTypes = List.of(Media.Format.IMAGE_JPEG, Media.Format.IMAGE_PNG, Media.Format.IMAGE_GIF, Media.Format.IMAGE_WEBP);

                    if(!allowMimeTypes.contains(mimeType)){
                        break;
                    }
                    String agentFileBase64 = fileService.getAgentFileBase64WithoutMineType(fileInfo.getId());
                    byte[] rawData = Base64.getDecoder().decode(agentFileBase64);

                    mediaList.add(Media.builder().name(fileInfo.getFilename()).mimeType(mimeType).data(rawData).build());
                }
            }
            Message message = switch (messageType) {
                case SYSTEM    -> SystemMessage.builder().build();
                case USER      -> UserMessage.builder().text(chatMessage.getContent()).media(mediaList).build();
                case ASSISTANT -> AssistantMessage.builder().content(chatMessage.getContent()).build();
                default        -> throw new IllegalArgumentException(
                        "Unsupported message type: " + messageType);
            };

            messages.add(message);
        }
        return Prompt.builder().messages(messages).build();
    }

    private ChatMessage handleSyncChat(OpenAiChatModel chatModel, Prompt prompt,ChatSession chatSession, ChatMessage askMessage, ChatMessage ansMessage ){
        ChatResponse chatResponse = chatModel.call(prompt);
        AssistantMessage output = chatResponse.getResult().getOutput();
        String content = output.getText();


        ansMessage.setContent(content)
                .setRole(MessageType.ASSISTANT.getValue())
                .setCreateTime(LocalDateTime.now());

        Object reasoningContentObj = output.getMetadata().get("reasoningContent");
        Object o = output.getMetadata().get("reasoning_content");
        if (Objects.nonNull(reasoningContentObj)) {
            ansMessage.setReasonContent(reasoningContentObj.toString());
        }

        Usage usage = chatResponse.getMetadata().getUsage();
        ansMessage.setTokenCount(usage.getTotalTokens());

        transactionTemplate.executeWithoutResult(status -> {
            chatSession.setUpdateTime(LocalDateTime.now());
            chatSessionMapper.updateById(chatSession);
            chatMessageMapper.insert(ansMessage);
        });

        QAMessage qaMessage = new QAMessage(
                UUIDUtils.randomUUID(),
                chatSession.getSessionId(),
                askMessage.getMessageId(),
                askMessage.getContent(),
                null,
                ansMessage.getMessageId(),
                ansMessage.getContent(),
                null,
                chatSession.getUserId(),
                LocalDateTime.now()
        );

        qaVectorRockerMqProductor.send(qaMessage);


        return ansMessage;
    }


    private ChatMessage handleStreamChatBackChatMessage(
            OpenAiChatModel chatModel, Prompt prompt,
            ChatSession chatSession, ChatMessage askMessage, ChatMessage ansMessage) {

        StringBuilder answer = new StringBuilder();
        StringBuilder answerReason = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        Disposable subscription = chatModel.stream(prompt).subscribe(
                // --- onNext ---
                chatResponse -> {
                    try {
                        if (Objects.nonNull(chatResponse.getMetadata().getUsage())) {
                            ansMessage.setTokenCount(
                                    chatResponse.getMetadata().getUsage().getTotalTokens());
                        }

                        AssistantMessage output = chatResponse.getResult().getOutput();
                        String text = output.getText();
                        Object reasoningContentObj = output.getMetadata().get("reasoningContent");

                        if (StringUtils.hasText(text)) {
                            answer.append(text);
                        }
                        if (Objects.nonNull(reasoningContentObj)) {
                            answerReason.append(reasoningContentObj.toString());
                        }
                    } catch (Exception e) {
                        // onNext 中的异常不会自动触发 onError，需手动捕获
                        log.error("处理流式chunk异常, sessionId={}", ansMessage.getSessionId(), e);
                    }
                },

                // --- onError ---
                error -> {
                    log.error("流式响应异常, sessionId={}", ansMessage.getSessionId(), error);
                    errorRef.set(error);
                    latch.countDown();
                },

                // --- onComplete ---
                () -> {
                    try {
                        ansMessage.setContent(answer.toString())
                                .setReasonContent(answerReason.toString())
                                .setRole(MessageType.ASSISTANT.getValue())
                                .setCreateTime(LocalDateTime.now());

                        transactionTemplate.executeWithoutResult(status -> {
                            chatMessageMapper.insert(ansMessage);
                            chatSession.setUpdateTime(LocalDateTime.now());
                            chatSessionMapper.updateById(chatSession);
                        });

                        QAMessage qaMessage = new QAMessage(
                                UUIDUtils.randomUUID(),
                                chatSession.getSessionId(),
                                askMessage.getMessageId(),
                                askMessage.getContent(),
                                null,
                                ansMessage.getMessageId(),
                                ansMessage.getContent(),
                                null,
                                chatSession.getUserId(),
                                LocalDateTime.now()
                        );
                        qaVectorRockerMqProductor.send(qaMessage);

                    } catch (Exception e) {
                        log.error("流结束持久化或MQ发送失败, sessionId={}",
                                ansMessage.getSessionId(), e);
                        errorRef.set(e);
                    } finally {
                        latch.countDown(); // ← 无论成功失败都必须释放
                    }
                }
        );

        // === 阻塞等待流完成 ===
        try {
            boolean completed = latch.await(10, TimeUnit.MINUTES);
            if (!completed) {
                subscription.dispose(); // 超时主动取消订阅，释放资源
                throw new IllegalStateException(
                        "流式响应超时(5min), sessionId=" + ansMessage.getSessionId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            subscription.dispose();
            throw new RuntimeException("等待流式响应被中断", e);
        }

        // 流过程中或持久化阶段有异常 → 抛出
        if (errorRef.get() != null) {
            throw new RuntimeException("流式处理失败, sessionId=" + ansMessage.getSessionId(),
                    errorRef.get());
        }

        return ansMessage; // ✅ 此时 content/reasonContent/tokenCount 已全部填充
    }


    private SseEmitter handleStreamChat(OpenAiChatModel chatModel, Prompt prompt,ChatSession chatSession, ChatMessage askMessage, ChatMessage ansMessage ) {
        SseEmitter emitter = new SseEmitter(300_000L);
        StringBuilder answer = new StringBuilder();       // ✅ OPT: StringBuffer → StringBuilder
        StringBuilder answerReason = new StringBuilder();
        AtomicReference<Disposable> subscriptionRef = new AtomicReference<>();


        Flux<ChatResponse> flux = chatModel.stream(prompt);



        Disposable subscription = flux.subscribe(
                // --- onNext: 处理每个增量 chunk ---
                chatResponse -> {

                    ChatMessage streamChunk = new ChatMessage()
                            .setMessageId(ansMessage.getMessageId())
                            .setSessionId(ansMessage.getSessionId());

                    if (Objects.nonNull(chatResponse.getMetadata().getUsage())) {
                        ChatResponseMetadata metadata = chatResponse.getMetadata();
                        Usage usage = metadata.getUsage();
                        if(usage.getTotalTokens()>0){
                            ansMessage.setTokenCount(usage.getTotalTokens());
                        }

                    }

                    Generation result = chatResponse.getResult();
                    if(Objects.isNull(result)){
                        return;
                    }
                    AssistantMessage output = result.getOutput();
                    String text = output.getText();

                    Object reasoningContentObj = output.getMetadata().get("reasoningContent");

                    boolean hasContent = StringUtils.hasText(text);
                    boolean hasReason = Objects.nonNull(reasoningContentObj);

                    // 过滤无效增量，避免前端收到空推送
                    if (!hasContent && !hasReason) {
                        return;
                    }

                    // 累积完整内容用于最终落库
                    if (hasContent) {
                        answer.append(text);
                    }
                    if (hasReason) {
                        answerReason.append(reasoningContentObj.toString());
                    }

                    try {
                        streamChunk.setContent(hasContent ? text : "")
                                .setReasonContent(hasReason ? reasoningContentObj.toString() : "");
                        streamChunk.setRole(MessageType.ASSISTANT.getValue());
                        emitter.send(HttpResult.back(streamChunk));
                    } catch (IOException e) {
                        // ✅ FIX: 客户端断开时主动取消上游流，防止 Token 浪费和资源泄漏
                        log.warn("SSE推送失败(客户端可能已断开)，取消上游LLM流, sessionId={}",
                                ansMessage.getSessionId(), e);
                        Disposable d = subscriptionRef.get();
                        if (d != null && !d.isDisposed()) {
                            d.dispose();
                        }
                    } catch (Exception e) {
                        log.error("SSE单次推送数据异常, sessionId={}", ansMessage.getSessionId(), e);
                        emitter.completeWithError(e);
                    }
                },

                // --- onError: 大模型返回致命错误 ---
                error -> {
                    log.error("流式响应处理异常, sessionId={}", ansMessage.getSessionId(), error);
                    emitter.completeWithError(error);
                },

                // --- onComplete: 流正常结束 ---
                () -> {
                    try {
                        // ✅ FIX: 流完全结束后才持久化，确保数据库写入完整内容
                        ansMessage.setContent(answer.toString())
                                .setReasonContent(answerReason.toString())
                                .setRole(MessageType.ASSISTANT.getValue())
                                .setCreateTime(LocalDateTime.now());

                        // ✅ FIX: 使用编程式事务保证落库原子性
                        transactionTemplate.executeWithoutResult(status -> {
                            chatMessageMapper.insert(ansMessage);
                            chatSession.setUpdateTime(LocalDateTime.now());
                            chatSessionMapper.updateById(chatSession);
                        });

                        QAMessage qaMessage = new QAMessage(
                                UUIDUtils.randomUUID(),
                                chatSession.getSessionId(),
                                askMessage.getMessageId(),
                                askMessage.getContent(),
                                null,
                                ansMessage.getMessageId(),
                                ansMessage.getContent(),
                                null,
                                chatSession.getUserId(),
                                LocalDateTime.now()
                        );

                        qaVectorRockerMqProductor.send(qaMessage);

                        // ✅ OPT: 结束标记统一使用 HttpResult 格式，避免前端特殊解析裸字符串
                        emitter.send(HttpResult.back("[DONE]"));
                        emitter.complete();
                    } catch (Exception e) {
                        log.error("流结束持久化或发送DONE标记失败, sessionId={}",
                                ansMessage.getSessionId(), e);
                        emitter.completeWithError(e);
                    }
                }
        );

        // ✅ FIX: 保存订阅引用，建立 Emitter ↔ Flux 双向生命周期绑定
        subscriptionRef.set(subscription);
        Runnable cancelUpstream = () -> {
            Disposable d = subscriptionRef.get();
            if (d != null && !d.isDisposed()) {
                d.dispose();
                log.info("SSE连接关闭，已取消上游LLM流式请求, sessionId={}", ansMessage.getSessionId());
            }
        };
        emitter.onCompletion(cancelUpstream);
        emitter.onTimeout(cancelUpstream);
        emitter.onError(t -> cancelUpstream.run());

        return emitter;
    }

}