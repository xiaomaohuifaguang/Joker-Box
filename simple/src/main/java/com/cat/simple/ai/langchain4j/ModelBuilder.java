package com.cat.simple.ai.langchain4j;

import com.cat.common.entity.ai.model.AiModel;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.stereotype.Component;


@Component
public class ModelBuilder {

    public ChatModel makeChatModel(AiModel aiModel){
        return OpenAiChatModel.builder()
                .apiKey(aiModel.getApiKey())
                .baseUrl(aiModel.getBaseUrl())
                .modelName(aiModel.getModel())
                .returnThinking(true)
                .temperature(1D)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    public StreamingChatModel makeStreamingChatModel(AiModel aiModel){
        return OpenAiStreamingChatModel.builder()
                .apiKey(aiModel.getApiKey())
                .baseUrl(aiModel.getBaseUrl())
                .modelName(aiModel.getModel())
                .returnThinking(true)
                .temperature(1D)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    public EmbeddingModel makeEmbeddingModel(AiModel aiModel){
        return OpenAiEmbeddingModel.builder().apiKey(aiModel.getApiKey())
                .baseUrl(aiModel.getBaseUrl())
                .modelName(aiModel.getModel())
                .dimensions(aiModel.getDimension())
                .build();
    }



}
