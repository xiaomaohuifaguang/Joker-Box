package com.cat.simple.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.cat.common.entity.HttpResult;
import com.cat.common.entity.ai.chat.ChatParam;
import com.cat.common.entity.ai.chat.res.ChatResponse;
import com.cat.common.entity.ai.model.AiModel;
import com.cat.common.utils.http.OkHttpUtils;
import com.cat.simple.ai.service.AiModelService;
import com.cat.simple.ai.service.AiChatService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.BufferedSource;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    private static final String API_PATH = "/v1/chat/completions";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Resource
    private AiModelService aiModelService;

    @Resource(name = "chatTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    public SseEmitter chat(ChatParam chatParam) {
        AiModel aiModel = aiModelService.getOneWithRealApiKeyById(chatParam.getModel());
        if (aiModel == null) {
            throw new RuntimeException("模型不存在");
        }

        String baseUrl = aiModel.getBaseUrl();
        String url = (baseUrl != null && baseUrl.endsWith("/"))
                ? baseUrl.substring(0, baseUrl.length() - 1) + API_PATH
                : baseUrl + API_PATH;

        ChatParam requestParam = new ChatParam()
                .setModel(aiModel.getModel())
                .setMessages(chatParam.getMessages())
                .setStream(chatParam.isStream());

        Map<String, String> headers = Map.of(AUTH_HEADER, BEARER_PREFIX + aiModel.getApiKey());
        SseEmitter emitter = new SseEmitter(0L);

        if (!chatParam.isStream()) {
            taskExecutor.execute(() -> handleSyncChat(url, requestParam, headers, emitter));
            return emitter;
        }

        handleStreamChat(url, requestParam, headers, emitter);
        return emitter;
    }

    private void handleSyncChat(String url, ChatParam requestParam, Map<String, String> headers, SseEmitter emitter) {
        try {
            String apiResult = OkHttpUtils.getInstance().postJson(url, requestParam, headers);
            ChatResponse chatResponse = JSON.parseObject(apiResult, ChatResponse.class);
            emitter.send(HttpResult.back(chatResponse));
            emitter.complete();
        } catch (Exception e) {
            log.error("非流式聊天请求失败", e);
            emitter.completeWithError(e);
        }
    }

    private void handleStreamChat(String url, ChatParam requestParam, Map<String, String> headers, SseEmitter emitter) {
        OkHttpUtils.getInstance().postJsonStreaming(url, requestParam, headers, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                log.error("流式聊天请求失败", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (IOException ex) {
                    log.warn("发送SSE error事件失败", ex);
                }
                emitter.completeWithError(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if (!response.isSuccessful()) {
                    String errorMsg = "AI接口错误: HTTP " + response.code();
                    log.error("{} , body: {}", errorMsg, readErrorBody(response));
                    emitter.completeWithError(new IOException(errorMsg));
                    return;
                }

                ResponseBody body = response.body();
                if (body == null) {
                    emitter.completeWithError(new IOException("响应体为空"));
                    return;
                }

                try (BufferedSource source = body.source()) {
                    while (!source.exhausted()) {
                        String line = source.readUtf8Line();
                        if (line == null || !line.startsWith("data:")) {
                            continue;
                        }
                        String jsonData = line.substring(5).trim();
                        if ("[DONE]".equals(jsonData)) {
                            continue;
                        }
                        ChatResponse data = JSON.parseObject(jsonData, ChatResponse.class);
                        emitter.send(SseEmitter.event().data(HttpResult.back(data)));
                    }
                    emitter.send(SseEmitter.event().data("[DONE]"));
                    emitter.complete();
                } catch (Exception e) {
                    log.error("流式响应处理异常", e);
                    emitter.completeWithError(e);
                }
            }
        });
    }

    private String readErrorBody(Response response) {
        ResponseBody body = response.body();
        if (body == null) {
            return "";
        }
        try {
            return body.string();
        } catch (IOException e) {
            return "";
        }
    }
}