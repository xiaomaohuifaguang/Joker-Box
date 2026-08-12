package com.cat.simple;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(proxyBeanMethods = false)
@EnableScheduling // 启用Spring的定时任务功能
@EnableAspectJAutoProxy // 启用Spring AOP的自动代理机制
@Slf4j
public class SimpleApplication {


    @Resource
    private RocketMQTemplate rocketMQTemplate;


    @PostConstruct
    private void test(){
        rocketMQTemplate.syncSend("pro-order-topic", "你好1");
        rocketMQTemplate.syncSend("pro-order-topic", "你好2");
        rocketMQTemplate.syncSend("pro-order-topic", "你好3");
    }

    public static void main(String[] args) {
        SpringApplication.run(SimpleApplication.class, args);

        
    }


}
