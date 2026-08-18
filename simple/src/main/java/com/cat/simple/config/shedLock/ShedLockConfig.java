package com.cat.simple.config.shedLock;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static net.javacrumbs.shedlock.provider.redis.support.InternalRedisLockProvider.DEFAULT_KEY_PREFIX;

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "30m")
public class ShedLockConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        // 第二参数是 Redis key 前缀，用来区分应用/环境
        return new RedisLockProvider(connectionFactory, DEFAULT_KEY_PREFIX,applicationName+":shedLock");
    }

}
