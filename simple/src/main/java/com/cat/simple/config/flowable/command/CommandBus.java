package com.cat.simple.config.flowable.command;

import com.cat.simple.config.flowable.guard.ProcessGuard;
import com.cat.simple.config.flowable.hook.ProcessLifecycleHook;
import com.cat.simple.config.flowable.recorder.HandleInfoRecorder;
import com.cat.simple.config.flowable.variable.ProcessVariableStore;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CommandBus {

    @Resource private ApplicationContext applicationContext;

    @Transactional(rollbackFor = Exception.class)
    public <T> T execute(ProcessCommand<T> command) {
        applicationContext.getAutowireCapableBeanFactory().autowireBean(command);
        return command.execute();
    }
}
