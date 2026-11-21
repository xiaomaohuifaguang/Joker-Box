package com.cat.simple.config.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("securityExecutor")
    public AsyncTaskExecutor securityExecutor() {
        ThreadPoolTaskExecutor delegate = new ThreadPoolTaskExecutor();
        delegate.setCorePoolSize(4);
        delegate.setMaxPoolSize(8);
        delegate.setQueueCapacity(100);
        delegate.setThreadNamePrefix("async-secure-");
        delegate.initialize();
        
        // ★★★ 关键就是这一行 ★★★
        return new DelegatingSecurityContextAsyncTaskExecutor(delegate);
    }
}
