package com.cat.simple.service.ai;

import com.cat.common.entity.ai.chat.ChatParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

public interface AiChatService {

    SseEmitter chat(ChatParam chatParam) throws IOException;

}
