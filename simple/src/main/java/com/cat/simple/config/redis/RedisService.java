package com.cat.simple.config.redis;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 存储
     * @param key key
     * @param value value
     * @param expire 过期时间（秒）
     */
    public void set(String key, Object value, long expire){
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(key, value, Duration.ofSeconds(expire));
    }

    public <T> T get(String key, Class<T> objectClass){
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        try {
            return objectClass.cast(ops.get(key));

        }catch (ClassCastException e){
            return null;
        }
    }

    public long incr(String key, long expire){
        Long seq = redisTemplate.opsForValue().increment(key);

        // 仅首次（seq==1）时设置过期，避免每次调用都执行 EXPIRE
        if (seq != null && seq == 1L) {
            redisTemplate.expire(key, 48, TimeUnit.SECONDS);
        }

        return Optional.ofNullable(seq).orElse(0L);
    }

    public void deleteKey(String key){
        redisTemplate.delete((key));
    }


}
