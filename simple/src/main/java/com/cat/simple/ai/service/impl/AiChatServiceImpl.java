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
import com.cat.common.entity.ai.systemPrompt.AiSystemPrompt;
import com.cat.common.entity.auth.LoginUser;
import com.cat.common.entity.file.FileInfo;
import com.cat.common.utils.UUIDUtils;
import com.cat.simple.ai.core.Role;
import com.cat.simple.ai.langchain4j.aiService.AiServicesBuilder;
import com.cat.simple.ai.langchain4j.aiService.CatAiService;
import com.cat.simple.ai.mapper.ChatMessageMapper;
import com.cat.simple.ai.mapper.ChatSessionMapper;
import com.cat.simple.ai.service.AiChatService;
import com.cat.simple.ai.service.AiModelService;
import com.cat.simple.ai.service.AiSystemPromptService;
import com.cat.simple.config.rocketmq.post.qa.QAVectorRockerMqProductor;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.file.service.FileService;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    @Resource
    private AiModelService aiModelService;

    @Resource
    private ChatSessionMapper chatSessionMapper;

    @Resource
    private ChatMessageMapper chatMessageMapper;

    @Resource
    private FileService fileService;

    @Resource
    private AiSystemPromptService aiSystemPromptService;

    @Resource
    private QAVectorRockerMqProductor qaVectorRockerMqProductor;


    @Resource
    private AiServicesBuilder aiServicesBuilder;

    private static final List<MimeType> ALLOW_IMAGE_TYPE = List.of(
            MimeType.valueOf("image/jpeg"),
            MimeType.valueOf("image/png"),
            MimeType.valueOf("image/gif"),
            MimeType.valueOf("image/webp")
    );


    private static final List<MimeType> ALLOW_DOC_TYPE = List.of(
            MimeType.valueOf("application/pdf"),
            MimeType.valueOf("application/msword"),
            MimeType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            MimeType.valueOf("application/vnd.ms-excel"),
            MimeType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            MimeType.valueOf("application/vnd.ms-powerpoint"),
            MimeType.valueOf("application/vnd.openxmlformats-officedocument.presentationml.presentation"));


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
                .setRole(Role.USER.name());

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
//            chatSession.setUpdateTime(now);
            // ✅ FIX: 上一版已修复为 getSessionId，此处保留；增加排序保证上下文顺序
            chatMessages = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getSessionId, chatSession.getSessionId())
                    .orderByAsc(ChatMessage::getId)
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

        }

        askMessage.setSessionId(chatSession.getSessionId());
        ansMessage.setSessionId(chatSession.getSessionId());

        chatMessages.add(askMessage);

        // 2. 构建大模型请求
        AiModel aiModel = aiModelService.getOneWithRealApiKeyById(chatRequestParam.getModelId());

        AiSystemPrompt defaultSystemPrompt = aiSystemPromptService.info(-1);

        CatAiService catAiService = aiServicesBuilder.makeAiService(aiModel, CatAiService.class, defaultSystemPrompt.getPrompt());



        List<Content> contents = new ArrayList<>();
        if(aiModel.getVision() && !CollectionUtils.isEmpty(askMessage.getFiles())){
            for (FileInfo fileInfo : askMessage.getFiles()) {
                MimeType mimeType = MimeType.valueOf(fileInfo.getContentType());
                if(!ALLOW_IMAGE_TYPE.contains(mimeType)){
                    break;
                }
                String agentFileBase64 = fileService.getAgentFileBase64WithoutMineType(fileInfo.getId());
                contents.add(ImageContent.from(agentFileBase64, fileInfo.getContentType()));
            }
        }



        List<FileInfo> docFileInfos = new ArrayList<>();
        if(!CollectionUtils.isEmpty(askMessage.getFiles())){
            for (FileInfo fileInfo : askMessage.getFiles()) {
                MimeType mimeType = MimeType.valueOf(fileInfo.getContentType());
                if(ALLOW_DOC_TYPE.contains(mimeType)){
                    docFileInfos.add(fileInfo);
                }
            }
        }
        String askContent = askMessage.getContent();
        if(!CollectionUtils.isEmpty(docFileInfos)){
            String listContent = docFileInfos.stream()
                    .map(info -> String.format("名称: %s, 文件id: %s", info.getFilename(), info.getId()))
                    .collect(Collectors.joining("\n")); // 用换行符拼接

            String formatted =
                    """
                    用户上传文件
                    %s
                    """.formatted(listContent);
            askContent += formatted;
        }


        InvocationParameters invocationParameters = InvocationParameters.from("userId", userId);

        // 3. 分流处理
        if (chatRequestParam.isStream()) {

            SseEmitter sseEmitter = new SseEmitter(300_000L);



            TokenStream tokenStream = catAiService.chatStream(chatSession.getSessionId(),askContent,contents, invocationParameters);

            ChatMessage chunkMessage = new ChatMessage();
            chunkMessage.setSessionId(ansMessage.getSessionId());
            chunkMessage.setMessageId(ansMessage.getMessageId());

            tokenStream.onToolExecuted(exec -> log.info("工具执行: {}", exec))       // ② 工具执行过程可观测
                    .onCompleteResponse(response -> {                            // ③ ⭐ 流式版的 Result
                        // ChatResponse ≈ Result 的元数据部分：
                        String fullText = response.aiMessage().text();           //   完整答案
                        TokenUsage usage  = response.tokenUsage();               //   token 用量（含工具轮次累计）
                        String thinking = response.aiMessage().thinking();

                        ansMessage.setContent(fullText);
                        ansMessage.setReasonContent(thinking);
                        ansMessage.setCreateTime(LocalDateTime.now());
                        ansMessage.setTokenCount(usage.totalTokenCount());
                        if(!StringUtils.hasText(chatRequestParam.getSessionId())){
                            chatSessionMapper.insert(chatSession);
                        }
                        chatMessageMapper.insert(askMessage);
                        chatMessageMapper.insert(ansMessage);
                        try {
                            sseEmitter.send(HttpResult.back("[DONE]"));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        sseEmitter.complete();

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

                    })
                    .onPartialResponseWithContext((partialResponse, partialResponseContext) -> {
                        chunkMessage.setContent(partialResponse.text());
                        chunkMessage.setReasonContent(null);
                        try {
                            sseEmitter.send(HttpResult.back(chunkMessage));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .onPartialThinkingWithContext((partialThinking, ctx) -> {
                        chunkMessage.setContent(null);
                        chunkMessage.setReasonContent(partialThinking.text());
                        try {
                            sseEmitter.send(HttpResult.back(chunkMessage));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .onError((throwable) -> {
                        log.info(throwable.getMessage());
                    })
                    .start();

            return sseEmitter;
        } else {

            Result<String> result = catAiService.chatResult(chatSession.getSessionId(), askContent, contents, invocationParameters);
            ansMessage.setContent(result.content());
            ansMessage.setReasonContent(result.finalResponse().aiMessage().thinking());
            ansMessage.setCreateTime(LocalDateTime.now());
            ansMessage.setTokenCount(result.tokenUsage().totalTokenCount());
            if(!StringUtils.hasText(chatRequestParam.getSessionId())){
                chatSessionMapper.insert(chatSession);
            }
            chatMessageMapper.insert(askMessage);
            chatMessageMapper.insert(ansMessage);
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
            return HttpResult.back(ansMessage);
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
        String contentType = file.getContentType();
        if(!StringUtils.hasText(contentType)){
            throw new IllegalStateException("未知类型上传");
        }
        MimeType mimeType = MimeType.valueOf(contentType);
        if(!ALLOW_IMAGE_TYPE.contains(mimeType) && !ALLOW_DOC_TYPE.contains(mimeType)){
            throw new IllegalStateException("文件类型不在解析范围内");
        }

        DTO<FileInfo> fileInfoDTO = fileService.uploadAgentFile(file);
        return fileInfoDTO.getData();
    }

    @Override
    public void fileDownload(String fileId) throws IOException {
        fileService.downloadAgentFile(fileId);
    }

}