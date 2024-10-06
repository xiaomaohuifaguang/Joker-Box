package com.cat.simple.config.redis;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

/***
 * redis 配置
 * @title RedisConfig
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/23 20:26
 **/
@Configuration
public class RedisConfig {

    /***
     * 存储  反序列化问题
     * @author xiaomaohuifaguang
     * @date 2023/8/10
     **/
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory){

        //创建RedisTemplate对象
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        //设置连接工厂
        template.setConnectionFactory(connectionFactory);
        //创建JSON序列化工具
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
        // 自定义配置
        jsonRedisSerializer.configure(objectMapper -> {
            objectMapper.registerModule(new JavaTimeModule());
        });
        //设置key的序列化
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        //设置value的序列化
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashKeySerializer(jsonRedisSerializer);
        //返回
        return template;
    }


}
