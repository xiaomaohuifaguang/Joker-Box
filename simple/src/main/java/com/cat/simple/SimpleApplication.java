package com.cat.simple;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(proxyBeanMethods = false)
@EnableScheduling // 启用Spring的定时任务功能
@EnableAspectJAutoProxy // 启用Spring AOP的自动代理机制
@Slf4j
public class SimpleApplication {




    public static void main(String[] args) {
        SpringApplication.run(SimpleApplication.class, args);

        
    }


}
