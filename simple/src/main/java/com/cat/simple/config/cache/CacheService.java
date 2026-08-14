package com.cat.simple.config.cache;

import com.cat.simple.config.cache.redis.RedisService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;


@Service
public class CacheService {

    @Resource
    private Environment env;


    @Resource
    private RedisService redisService;

    /**
     * key 统一前缀（应用名），启动时解析一次，避免每次读写都查 Environment
     */
    private String keyPrefix;

    @PostConstruct
    void initKeyPrefix(){
        String applicationName = env.getProperty("spring.application.name");
        this.keyPrefix = StringUtils.hasText(applicationName) ? applicationName + ":" : "";
    }

    private String makeKey(String key){
        return keyPrefix + key;
    }

    /**
     * 过期时间加正向随机抖动（0 ~ +10%），避免同批写入的 key 在同一时刻集中过期引发雪崩
     */
    private long jitter(long expire){
        return expire + ThreadLocalRandom.current().nextLong(expire / 10 + 1);
    }

    public void set(CacheKeyEnum cacheKeyEnum, String suffixStr, Object value){
        set(cacheKeyEnum.getPrefix() + suffixStr, value, jitter(cacheKeyEnum.getExpire()));
    }

    public void set(CacheKeyEnum cacheKeyEnum, Object value){
        set(cacheKeyEnum.getPrefix(), value, jitter(cacheKeyEnum.getExpire()));
    }

    public void set(String key, Object value, long expire){
        redisService.set(makeKey(key), value, expire);
    }

    public <T> T get(CacheKeyEnum cacheKeyEnum, String suffixStr, Class<T> objectClass){
        return get(cacheKeyEnum.getPrefix() + suffixStr, objectClass);
    }


    public <T> T get(CacheKeyEnum cacheKeyEnum, Class<T> objectClass){
        return get(cacheKeyEnum.getPrefix(), objectClass);
    }

    public <T> T get(String key, Class<T> objectClass){
        return redisService.get(makeKey(key), objectClass);
    }


    public <T> List<T> getList(CacheKeyEnum cacheKeyEnum, String suffixStr, Class<T> objectClass){
        return getList(cacheKeyEnum.getPrefix() + suffixStr, objectClass);
    }


    public <T>List<T> getList(CacheKeyEnum cacheKeyEnum, Class<T> objectClass){
        return getList(cacheKeyEnum.getPrefix(), objectClass);
    }

    public <T> List<T> getList(String key, Class<T> objectClass){
        return redisService.getList(makeKey(key), objectClass);
    }


    public void deleteKey(CacheKeyEnum cacheKeyEnum, String suffixStr){
        deleteKey(cacheKeyEnum.getPrefix()+suffixStr);
    }

    public void deleteKey(CacheKeyEnum cacheKeyEnum){
        deleteKey(cacheKeyEnum.getPrefix());
    }

    public void deleteKey(String key){
        redisService.deleteKey(makeKey(key));
    }

    public long incr(CacheKeyEnum cacheKeyEnum, String suffix){
        return incr(cacheKeyEnum.getPrefix() + suffix, jitter(cacheKeyEnum.getExpire()));
    }

    public long incr(CacheKeyEnum cacheKeyEnum){
        return incr(cacheKeyEnum.getPrefix(), jitter(cacheKeyEnum.getExpire()));
    }

    public long incr(String key, long expire){
        return redisService.incr(makeKey(key), expire);
    }

    /* ========== Hash 操作（key 级 TTL，整个 hash 共用枚举过期时间） ========== */

    public void hset(CacheKeyEnum cacheKeyEnum, String suffixStr, String field, Object value){
        redisService.hset(makeKey(cacheKeyEnum.getPrefix() + suffixStr), field, value,
                jitter(cacheKeyEnum.getExpire()));
    }

    public void hset(CacheKeyEnum cacheKeyEnum, String field, Object value){
        hset(cacheKeyEnum, "", field, value);
    }

    public <T> T hget(CacheKeyEnum cacheKeyEnum, String suffixStr, String field, Class<T> objectClass){
        return redisService.hget(makeKey(cacheKeyEnum.getPrefix() + suffixStr), field, objectClass);
    }

    public <T> T hget(CacheKeyEnum cacheKeyEnum, String field, Class<T> objectClass){
        return hget(cacheKeyEnum, "", field, objectClass);
    }

    public <T> List<T> hgetList(CacheKeyEnum cacheKeyEnum, String suffixStr, String field, Class<T> elementClass){
        return redisService.hgetList(makeKey(cacheKeyEnum.getPrefix() + suffixStr), field, elementClass);
    }

    public <T> List<T> hgetList(CacheKeyEnum cacheKeyEnum, String field, Class<T> elementClass){
        return hgetList(cacheKeyEnum, "", field, elementClass);
    }

    public Map<String, Object> hgetAll(CacheKeyEnum cacheKeyEnum, String suffixStr){
        return redisService.hgetAll(makeKey(cacheKeyEnum.getPrefix() + suffixStr));
    }

    public Map<String, Object> hgetAll(CacheKeyEnum cacheKeyEnum){
        return hgetAll(cacheKeyEnum, "");
    }

    public void hdel(CacheKeyEnum cacheKeyEnum, String suffixStr, String... fields){
        redisService.hdel(makeKey(cacheKeyEnum.getPrefix() + suffixStr), fields);
    }

    /**
     * 删除单个字段（无后缀）。与上面的变参版本区分，避免重载歧义
     */
    public void hdel(CacheKeyEnum cacheKeyEnum, String field){
        redisService.hdel(makeKey(cacheKeyEnum.getPrefix()), field);
    }

    public long hincr(CacheKeyEnum cacheKeyEnum, String suffixStr, String field, long delta){
        return redisService.hincr(makeKey(cacheKeyEnum.getPrefix() + suffixStr), field, delta);
    }

    public long hincr(CacheKeyEnum cacheKeyEnum, String field, long delta){
        return hincr(cacheKeyEnum, "", field, delta);
    }

    public boolean hHasKey(CacheKeyEnum cacheKeyEnum, String suffixStr, String field){
        return redisService.hHasKey(makeKey(cacheKeyEnum.getPrefix() + suffixStr), field);
    }

    public boolean hHasKey(CacheKeyEnum cacheKeyEnum, String field){
        return hHasKey(cacheKeyEnum, "", field);
    }

}
