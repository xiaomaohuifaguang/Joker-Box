package com.cat.simple.ai.controller;


import com.cat.common.entity.HttpResult;
import com.cat.common.entity.ai.chat.ChatMessage;
import com.cat.common.entity.ai.chat.ChatRequestParam;
import com.cat.common.entity.ai.chat.ChatSession;
import com.cat.common.entity.ai.model.AiModel;
import com.cat.simple.ai.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/ai/completions")
@Tag(name = "ai接口")
public class AiController {


    @Resource
    private AiChatService aiChatService;


    @Operation(summary = "聊天")
    @RequestMapping(value = "/chat", method = RequestMethod.POST)
    public Object chat(@RequestBody ChatRequestParam chatRequestPram) throws IOException {
        return aiChatService.chat(chatRequestPram);
    }

    @Operation(summary = "会话列表")
    @RequestMapping(value = "/sessions", method = RequestMethod.POST)
    public HttpResult<List<ChatSession>> sessions() {
        return HttpResult.back(aiChatService.sessions());
    }

    @Operation(summary = "会话消息列表")
    @Parameters(
            @Parameter(name = "sessionId",description = "会话id",required = true)
    )
    @RequestMapping(value = "/messages", method = RequestMethod.POST)
    public HttpResult<List<ChatMessage>> messages(@RequestParam("sessionId") String sessionId) {
        return HttpResult.back(aiChatService.messages(sessionId));
    }


    @Operation(summary = "模型列表")
    @RequestMapping(value = "/models", method = RequestMethod.POST)
    public HttpResult<List<AiModel>> chatModels() {
        return HttpResult.back(aiChatService.chatModels());
    }


}
