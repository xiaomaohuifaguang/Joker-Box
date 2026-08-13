package com.cat.simple.config.flowable.command;


import jakarta.annotation.Resource;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 命令总线，负责将流程命令注入 Spring 依赖并在一个事务中执行。
 */
@Component
public class CommandBus {

    @Resource private ApplicationContext applicationContext;

    /** 执行流程命令，自动注入依赖并开启事务。 */
    public <T> T execute(ProcessCommand<T> command) {
        applicationContext.getAutowireCapableBeanFactory().autowireBean(command);
        return command.execute();
    }
}
