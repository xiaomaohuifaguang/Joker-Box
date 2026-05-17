package com.cat.simple.ai.controller;


import com.cat.common.entity.ai.chat.ChatParam;
import com.cat.simple.ai.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
@Tag(name = "ai接口")
public class AiController {


    @Resource
    private AiChatService aiChatService;


    @Operation(summary = "聊天")
    @RequestMapping(value = "/chat", method = RequestMethod.POST)
    public SseEmitter chat(@RequestBody ChatParam chatParam) throws IOException {
        return aiChatService.chat(chatParam);
    }




}
