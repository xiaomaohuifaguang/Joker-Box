package com.cat.simple.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.HttpResult;
import com.cat.common.entity.ai.chat.ChatMessage;
import com.cat.common.entity.ai.chat.ChatRequestParam;
import com.cat.common.entity.ai.chat.ChatSession;
import com.cat.common.entity.ai.model.AiModel;
import com.cat.common.entity.ai.model.ModelType;
import com.cat.common.utils.UUIDUtils;
import com.cat.simple.ai.mapper.ChatMessageMapper;
import com.cat.simple.ai.mapper.ChatSessionMapper;
import com.cat.simple.ai.service.AiChatService;
import com.cat.simple.ai.service.AiModelService;
import com.cat.simple.ai.service.LlmService;
import com.cat.simple.config.security.SecurityUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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

    // ✅ OPT: 移除未使用的 okhttp3.* 和 ThreadPoolTaskExecutor 导入/注入

    @Override
    // ✅ FIX: 移除 @Transactional。该方法包含异步流式分支，声明式事务会导致连接泄漏和数据不一致
    public Object chat(ChatRequestParam chatRequestParam) {

        String userId = Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId();
        LocalDateTime now = LocalDateTime.now();
        ChatSession chatSession;
        List<ChatMessage> chatMessages = new ArrayList<>();

        ChatMessage askMessage = new ChatMessage()
                .setMessageId(UUIDUtils.randomUUID())
                .setCreateTime(now)
                .setContent(chatRequestParam.getContent())
                .setRole(OpenAiApi.ChatCompletionMessage.Role.USER.name());

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
        OpenAiApi openAiApi = llmService.buildOpenAiApi(aiModel);
//        OpenAiChatModel build = OpenAiChatModel.builder().openAiApi(openAiApi).build();
        List<OpenAiApi.ChatCompletionMessage> messages = buildMessageList(chatMessages);
        OpenAiApi.ChatCompletionRequest request = new OpenAiApi.ChatCompletionRequest(
                messages, aiModel.getModel(), 1d, chatRequestParam.isStream()
        );

        // 3. 分流处理
        if (chatRequestParam.isStream()) {
            request = request.streamOptions(new OpenAiApi.ChatCompletionRequest.StreamOptions(true));
            return handleStreamChat(request, openAiApi, ansMessage, chatSession);
        } else {
            return HttpResult.back(handleSyncChat(request, openAiApi, ansMessage, chatSession));
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

    /**
     * ✅ FIX: 流式聊天独立方法，彻底解决事务、落库时机、资源泄漏三大问题
     */
    private SseEmitter handleStreamChat(OpenAiApi.ChatCompletionRequest request,
                                        OpenAiApi openAiApi,
                                        ChatMessage ansMessage,
                                        ChatSession chatSession) {
        SseEmitter emitter = new SseEmitter(300_000L);
        StringBuilder answer = new StringBuilder();       // ✅ OPT: StringBuffer → StringBuilder
        StringBuilder answerReason = new StringBuilder();
        AtomicReference<Disposable> subscriptionRef = new AtomicReference<>();

        Flux<OpenAiApi.ChatCompletionChunk> flux = openAiApi.chatCompletionStream(request);

        Disposable subscription = flux.subscribe(
                // --- onNext: 处理每个增量 chunk ---
                chunk -> {

//                    log.info("RAW CHUNK: {}", chunk);

                    ChatMessage streamChunk = new ChatMessage()
                            .setMessageId(ansMessage.getMessageId())
                            .setSessionId(ansMessage.getSessionId());

                    if (Objects.nonNull(chunk.usage())) {
                        ansMessage.setTokenCount(chunk.usage().totalTokens());
                    }

                    // ✅ FIX: 防御 choices 为空导致 IndexOutOfBoundsException
                    if (CollectionUtils.isEmpty(chunk.choices())) {
                        return;
                    }
                    OpenAiApi.ChatCompletionMessage delta = chunk.choices().get(0).delta();
                    if (Objects.isNull(delta)) {
                        return;
                    }

                    boolean hasContent = StringUtils.hasText(delta.content());
                    boolean hasReason = StringUtils.hasText(delta.reasoningContent());

                    // 过滤无效增量，避免前端收到空推送
                    if (!hasContent && !hasReason) {
                        return;
                    }

                    // 累积完整内容用于最终落库
                    if (hasContent) {
                        answer.append(delta.content());
                    }
                    if (hasReason) {
                        answerReason.append(delta.reasoningContent());
                    }

                    // 构建增量推送对象（仅设置当前片段，符合打字机需求）
                    try {
                        streamChunk.setContent(hasContent ? delta.content() : "")
                                .setReasonContent(hasReason ? delta.reasoningContent() : "");
                        // ✅ FIX: role 仅在首个 chunk 存在，后续为 null，需判空防 NPE
                        if (Objects.nonNull(delta.role())) {
                            streamChunk.setRole(delta.role().name());
                        }
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
                                .setRole(OpenAiApi.ChatCompletionMessage.Role.ASSISTANT.name())
                                .setCreateTime(LocalDateTime.now());

                        // ✅ FIX: 使用编程式事务保证落库原子性
                        transactionTemplate.executeWithoutResult(status -> {
                            chatMessageMapper.insert(ansMessage);
                            chatSession.setUpdateTime(LocalDateTime.now());
                            chatSessionMapper.updateById(chatSession);
                        });

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

    /**
     * ✅ OPT: 非流式聊天独立方法，职责清晰，可安全使用声明式事务
     */
    private ChatMessage handleSyncChat(OpenAiApi.ChatCompletionRequest request,
                                       OpenAiApi openAiApi,
                                       ChatMessage ansMessage,
                                       ChatSession chatSession) {
        ResponseEntity<OpenAiApi.ChatCompletion> responseEntity = openAiApi.chatCompletionEntity(request);
        OpenAiApi.ChatCompletion body = responseEntity.getBody();

        if (Objects.isNull(body) || CollectionUtils.isEmpty(body.choices())) {
            // ✅ FIX: 非流式同样增加 choices 边界检查
            throw new IllegalStateException("大模型返回结果为空");
        }

        OpenAiApi.ChatCompletionMessage message = body.choices().get(0).message();
        ansMessage.setContent(message.content())
                .setRole(message.role().name())
                .setCreateTime(LocalDateTime.now());

        if (StringUtils.hasText(message.reasoningContent())) {
            ansMessage.setReasonContent(message.reasoningContent());
        }
        if (Objects.nonNull(body.usage())) {
            ansMessage.setTokenCount(body.usage().totalTokens());
        }

        transactionTemplate.executeWithoutResult(status -> {
            chatSession.setUpdateTime(LocalDateTime.now());
            chatSessionMapper.updateById(chatSession);
            chatMessageMapper.insert(ansMessage);
        });
        return ansMessage;
    }

    /**
     * ✅ FIX: 返回空列表而非 null，避免下游 NPE
     */
    private List<OpenAiApi.ChatCompletionMessage> buildMessageList(List<ChatMessage> chatMessages) {
        if (CollectionUtils.isEmpty(chatMessages)) {
            return Collections.emptyList();
        }
        List<OpenAiApi.ChatCompletionMessage> messages = new ArrayList<>(chatMessages.size()+1);
        messages.add(new OpenAiApi.ChatCompletionMessage(
                "你是一个专业的AI助手，请严格遵循以下角色设定进行对话：\n" +
                        "\n" +
                        "## 角色身份\n" +
                        "- 名称：希斯莱杰\n" +
                        "- 身份：小丑\n" +
                        "- 性格特征：搞怪，癫狂，说一些奇怪的话\n" +
                        "\n" +
                        "## 行为规范\n" +
                        "1. 始终以第一人称\"希斯莱杰\"的口吻回复，不要暴露AI身份\n" +
                        "2. 保持角色一致性，不跳出人设，不使用\"作为AI\"等表述\n" +
                        "3. 回复风格应符合角色性格：搞怪，癫狂，说一些奇怪的话\n" +
                        "4. 对超出角色知识范围的问题，用符合人设的方式委婉回应，而非直接拒绝\n" +
                        "\n" +
                        "## 对话约束\n" +
                        "- 语言：中文\n" +
//                        "- 单次回复长度：不超过2000字\n" +
                        "- 禁止输出任何与角色无关的元信息、解释或免责声明",
                OpenAiApi.ChatCompletionMessage.Role.SYSTEM)
        );
        for (ChatMessage chatMessage : chatMessages) {
            OpenAiApi.ChatCompletionMessage message = new OpenAiApi.ChatCompletionMessage(
                    chatMessage.getContent(),
                    OpenAiApi.ChatCompletionMessage.Role.valueOf(chatMessage.getRole())
            );
            messages.add(message);
        }
        return messages;
    }
}