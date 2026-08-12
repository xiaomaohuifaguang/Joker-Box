package com.cat.simple.config.cache;

import com.cat.simple.config.redis.RedisService;
import jakarta.annotation.Resource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


@Service
public class CacheService {

    @Resource
    private Environment env;


    @Resource
    private RedisService redisService;

    private String makeKey(String key){
        String applicationName = env.getProperty("spring.application.name");
        return StringUtils.hasText(applicationName) ? applicationName+":"+key : key;
    }

    public void set(CacheKeyEnum cacheKeyEnum, String suffixStr, Object value){
        set(cacheKeyEnum.getPrefix() + suffixStr, value,cacheKeyEnum.getExpire());
    }

    public void set(CacheKeyEnum cacheKeyEnum, Object value){
        set(cacheKeyEnum.getPrefix(), value,cacheKeyEnum.getExpire());
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
        return incr(cacheKeyEnum.getPrefix() + suffix, cacheKeyEnum.getExpire());
    }

    public long incr(CacheKeyEnum cacheKeyEnum){
        return incr(cacheKeyEnum.getPrefix(), cacheKeyEnum.getExpire());
    }

    public long incr(String key, long expire){
        return redisService.incr(makeKey(key), expire);
    }

}
