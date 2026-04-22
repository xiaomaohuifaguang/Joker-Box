package com.cat.simple.service.ai;

import com.cat.common.entity.HttpResult;
import com.cat.common.entity.ai.chat.ChatParam;
import com.cat.common.entity.ai.chat.res.ChatResponse;

import java.io.IOException;

public interface AiChatService {




    void chat(ChatParam chatParam) throws IOException;





}
