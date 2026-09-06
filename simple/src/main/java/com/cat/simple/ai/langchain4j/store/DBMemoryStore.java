package com.cat.simple.ai.langchain4j.store;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.ai.model.AiMemory;
import com.cat.simple.ai.mapper.AiMemoryMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class DBMemoryStore implements ChatMemoryStore {

    @Resource
    private AiMemoryMapper aiMemoryMapper;



    @Override
    public List<ChatMessage> getMessages(Object memoryId) {

        AiMemory aiMemory = aiMemoryMapper.selectOne(new LambdaQueryWrapper<AiMemory>().eq(AiMemory::getSessionId, memoryId));
        if(Objects.nonNull(aiMemory) && StringUtils.hasText(aiMemory.getMessages())){
            List<ChatMessage> chatMessages = ChatMessageDeserializer.messagesFromJson(aiMemory.getMessages());
            return chatMessages;
        }else {
            return new ArrayList<>();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String messagesStr = ChatMessageSerializer.messagesToJson(messages);
        AiMemory aiMemory = aiMemoryMapper.selectOne(new LambdaQueryWrapper<AiMemory>().eq(AiMemory::getSessionId, memoryId));
        if(Objects.nonNull(aiMemory)){
            aiMemory.setMessages(messagesStr);
            aiMemoryMapper.updateById(aiMemory);
        }else {
            aiMemory = new AiMemory();
            aiMemory.setSessionId(String.valueOf(memoryId));
            aiMemory.setMessages(messagesStr);
            aiMemoryMapper.insert(aiMemory);
        }

    }

    @Override
    public void deleteMessages(Object memoryId) {
        aiMemoryMapper.delete(new LambdaQueryWrapper<AiMemory>().eq(AiMemory::getSessionId, memoryId));
    }
}
