package com.cat.simple.config.cache.redis;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    /**
     * INCR + 首次 EXPIRE 原子执行，避免两条命令之间进程崩溃导致 key 永不过期
     */
    private static final DefaultRedisScript<Long> INCR_WITH_EXPIRE_SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('INCR', KEYS[1]) " +
            "if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
            "return current",
            Long.class
    );

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate; // 脚本专用：参数按纯字符串序列化，避免 JSON 加引号
    @Resource
    private ObjectMapper objectMapper; // ✅ 使用 Spring Boot 自动配置的实例

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
        return get(key, objectMapper.getTypeFactory().constructType(objectClass));
    }

    public <T> T get(String key, JavaType javaType) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        Object value = ops.get(key);
        if (value == null) {
            return null;
        }
        return objectMapper.convertValue(value, javaType); // ✅ 替换 new ObjectMapper()
    }

    public <T> List<T> getList(String key, Class<T> elementClass) {
        JavaType javaType = objectMapper.getTypeFactory()       // ✅ 同样替换
                .constructCollectionType(List.class, elementClass);
        return get(key, javaType);
    }

    /**
     * 自增并在首次（值为 1 时）设置过期时间，INCR 与 EXPIRE 通过 Lua 脚本原子执行
     */
    public long incr(String key, long expire){
        Long seq = stringRedisTemplate.execute(INCR_WITH_EXPIRE_SCRIPT,
                Collections.singletonList(key), String.valueOf(expire));
        return seq == null ? 0L : seq;
    }

    public void deleteKey(String key){
        redisTemplate.delete((key));
    }

    /* ========== Hash 操作（TTL 为 key 级，整个 hash 共用一个过期时间） ========== */

    /**
     * 写 hash 字段，不设置/不刷新过期时间
     */
    public void hset(String key, String field, Object value){
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * 写 hash 字段，仅当 key 尚无过期时间时设置 TTL。
     * 避免每次改字段都刷新整个 hash 寿命，导致冷字段永不失效。
     */
    public void hset(String key, String field, Object value, long expire){
        HashOperations<String, String, Object> ops = redisTemplate.opsForHash();
        ops.put(key, field, value);
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttl != null && ttl == -1L) {
            redisTemplate.expire(key, Duration.ofSeconds(expire));
        }
    }

    public <T> T hget(String key, String field, Class<T> objectClass){
        return hget(key, field, objectMapper.getTypeFactory().constructType(objectClass));
    }

    public <T> T hget(String key, String field, JavaType javaType){
        Object value = redisTemplate.opsForHash().get(key, field);
        if (value == null) {
            return null;
        }
        return objectMapper.convertValue(value, javaType);
    }

    public <T> List<T> hgetList(String key, String field, Class<T> elementClass){
        JavaType javaType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, elementClass);
        return hget(key, field, javaType);
    }

    /**
     * 读整个 hash，value 为 JSON 反序列化后的原始对象（Map/List/基本类型）
     */
    public Map<String, Object> hgetAll(String key){
        HashOperations<String, String, Object> ops = redisTemplate.opsForHash();
        return ops.entries(key);
    }

    public void hdel(String key, String... fields){
        redisTemplate.opsForHash().delete(key, (Object[]) fields);
    }

    public long hincr(String key, String field, long delta){
        Long val = redisTemplate.opsForHash().increment(key, field, delta);
        return val == null ? 0L : val;
    }

    public boolean hHasKey(String key, String field){
        return redisTemplate.opsForHash().hasKey(key, field);
    }


}
