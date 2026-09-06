package com.cat.simple;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

import java.util.List;

interface Assistant {

    String chat(String userMessage);

    ChatResponse chatResponse(@UserMessage String userMessage);

    Result<String> chatResult(@UserMessage String userMessage);

    TokenStream chatStream(@UserMessage String userMessage, @UserMessage List<Content> contents);

}