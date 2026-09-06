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
import org.springframework.util.StringUtils;

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



    public <T> T makeAiService(AiModel aiModel, Class<T> aiService, String systemPrompt){
        ChatModel chatModel = modelBuilder.makeChatModel(aiModel);
        StreamingChatModel streamingChatModel = modelBuilder.makeStreamingChatModel(aiModel);
        return makeAiService(chatModel, streamingChatModel, aiService, systemPrompt);
    }


    public <T> T makeAiService(ChatModel chatModel, StreamingChatModel streamingChatModel, Class<T> aiService, String systemPrompt){
        AiServices<T> builder = AiServices.builder(aiService);
        builder.chatModel(chatModel);
        builder.streamingChatModel(streamingChatModel);
        if(StringUtils.hasText(systemPrompt)){
            builder.systemMessage(systemPrompt);
        }
        builder.chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder().id(memoryId).chatMemoryStore(store).alwaysKeepSystemMessageFirst(true).maxMessages(20).build());
        builder.tools(weatherTools, systemTools, fileParseTools);
        return builder.build();
    }


}
