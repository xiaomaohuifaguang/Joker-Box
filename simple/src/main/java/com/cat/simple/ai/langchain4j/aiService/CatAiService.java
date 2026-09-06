package com.cat.simple.ai.langchain4j.aiService;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

import java.util.List;

public interface CatAiService {

    Result<String> chatResult(@MemoryId String memoryId, @UserMessage String userMessage, @UserMessage List<Content> contents, InvocationParameters parameters);

    TokenStream chatStream(@MemoryId String memoryId, @UserMessage String userMessage, @UserMessage List<Content> contents, InvocationParameters parameters);

}
