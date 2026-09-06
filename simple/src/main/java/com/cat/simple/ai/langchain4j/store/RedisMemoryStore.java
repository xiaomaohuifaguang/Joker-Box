package com.cat.simple.ai.langchain4j.store;

import com.cat.simple.config.cache.CacheService;
import dev.langchain4j.data.message.*;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RedisMemoryStore implements ChatMemoryStore {

    @Resource
    private CacheService cacheService;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        // 从 Redis 取出 JSON 字符串
        String json = cacheService.get(String.valueOf(memoryId), String.class);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        // 使用官方反序列化器，能正确还原子类型

        return ChatMessageDeserializer.messagesFromJson(json);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        // 序列化为带类型信息的 JSON 字符串存储
        String json = ChatMessageSerializer.messagesToJson(messages);
        cacheService.set(String.valueOf(memoryId), json, 30 * 60);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        cacheService.deleteKey(String.valueOf(memoryId));
    }
}
