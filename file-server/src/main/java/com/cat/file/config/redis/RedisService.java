package com.cat.file.config.redis;

import jakarta.annotation.Resource;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

/***
 * redis 具体实现方法
 * @title RedisService
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/24 23:28
 **/
@Service
public class RedisService {

    @Resource
    private Environment env;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 存储
     * @param key key
     * @param value value
     * @param expire 过期时间（秒）
     */
    public void set(String key, Object value, long expire){
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(makeKey(key), value, Duration.ofSeconds(expire));
    }

    public <T> T get(String key, Class<T> objectClass){
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        return objectClass.cast(ops.get(makeKey(key)));
    }

    private String makeKey(String key){
        String applicationName = env.getProperty("spring.application.name");
        return StringUtils.hasText(applicationName) ? applicationName+":"+key : key;
    }


}
