package com.cat.ai.service;

import com.cat.common.entity.ai.chat.ChatRequestPram;

import java.io.IOException;

/***
 * <TODO description class purpose>
 * @title OpenAiService
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/8/2 21:36
 **/
public interface OpenAiService {

    /**
     * /v1/chat/completions
     */
    void chat(ChatRequestPram chatRequestPram) throws IOException;


}
