package com.cat.simple.ai.langchain4j.aiService;

import com.cat.common.entity.ai.model.AiModel;
import com.cat.simple.ai.langchain4j.ModelBuilder;
import com.cat.simple.ai.langchain4j.skill.SkillsBuilder;
import com.cat.simple.ai.langchain4j.store.DBMemoryStore;
import com.cat.simple.ai.tools.file.FileParseTools;
import com.cat.simple.ai.tools.system.CommandTools;
import com.cat.simple.ai.tools.system.SystemTools;
import com.cat.simple.ai.tools.weather.WeatherTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.skills.Skill;
import dev.langchain4j.skills.Skills;
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
    private CommandTools commandTools;
    @Resource
    private FileParseTools fileParseTools;
    @Resource
    private ModelBuilder modelBuilder;

    @Resource
    private Skill emojiSkill;

    @Resource
    private SkillsBuilder skillsBuilder;



    public <T> T makeAiService(AiModel aiModel, Class<T> aiService, String systemPrompt){
        ChatModel chatModel = modelBuilder.makeChatModel(aiModel);
        StreamingChatModel streamingChatModel = modelBuilder.makeStreamingChatModel(aiModel);
        return makeAiService(chatModel, streamingChatModel, aiService, systemPrompt);
    }


    public <T> T makeAiService(ChatModel chatModel, StreamingChatModel streamingChatModel, Class<T> aiService, String systemPrompt){

        Skills skills = skillsBuilder.makeSkills(emojiSkill);

        AiServices<T> builder = AiServices.builder(aiService);
        builder.chatModel(chatModel);
        builder.streamingChatModel(streamingChatModel);
        if(StringUtils.hasText(systemPrompt)){
            builder.systemMessage(systemPrompt+"\nYou have access to the following skills:\n"+skills.formatAvailableSkills());
        }else {
            builder.systemMessage("You have access to the following skills:\n"+skills.formatAvailableSkills());
        }
        builder.chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder().id(memoryId).chatMemoryStore(store).alwaysKeepSystemMessageFirst(true).maxMessages(30).build());
        builder.tools(systemTools, commandTools, fileParseTools);
        builder.toolProvider(skills.toolProvider());
        builder.maxToolCallingRoundTrips(10);
        return builder.build();
    }


}
