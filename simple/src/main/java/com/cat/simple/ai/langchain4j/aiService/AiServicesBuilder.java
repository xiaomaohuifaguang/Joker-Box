package com.cat.simple.ai.langchain4j.aiService;

import com.cat.common.entity.ai.model.AiModel;
import com.cat.simple.ai.langchain4j.ModelBuilder;
import com.cat.simple.ai.langchain4j.store.DBMemoryStore;
import com.cat.simple.ai.tools.file.FileParseTools;
import com.cat.simple.ai.tools.system.SystemTools;
import com.cat.simple.ai.tools.weather.WeatherTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class AiServicesBuilder {

    @Resource
    private DBMemoryStore store;
    @Resource
    private WeatherTools weatherTools;
    @Resource
    private SystemTools systemTools;
    @Resource
    private FileParseTools fileParseTools;
    @Resource
    private ModelBuilder modelBuilder;

    public <T> T makeAiService(AiModel aiModel, Class<T> aiService){
        ChatModel chatModel = modelBuilder.makeChatModel(aiModel);
        StreamingChatModel streamingChatModel = modelBuilder.makeStreamingChatModel(aiModel);

        return AiServices.builder(aiService)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder().id(memoryId).chatMemoryStore(store).maxMessages(10).build())
                .tools(weatherTools, systemTools, fileParseTools).build();
    }


    public <T> T makeAiService(ChatModel chatModel, StreamingChatModel streamingChatModel, Class<T> aiService){
        return AiServices.builder(aiService)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder().id(memoryId).chatMemoryStore(store).maxMessages(10).build())
                .tools(weatherTools, systemTools, fileParseTools).build();
    }


}
