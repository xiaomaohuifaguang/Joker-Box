package com.cat.simple.config.flowable.command;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CommandBus {

    @Transactional(rollbackFor = Exception.class)
    public <T> T execute(ProcessCommand<T> command) {
        return command.execute();
    }
}
