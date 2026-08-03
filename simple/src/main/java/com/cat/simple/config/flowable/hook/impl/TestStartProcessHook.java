package com.cat.simple.config.flowable.hook.impl;

import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.hook.ProcessLifecycleHook;
import com.cat.simple.config.flowable.hook.context.*;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.api.Task;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(2)
public class TestStartProcessHook implements ProcessLifecycleHook {

    @Override
    public void beforeStart(StartContext ctx) {
        log.debug("测试hook-beforeStart");
    }

    @Override
    public void afterStart(StartContext ctx) {
        log.debug("测试hook-afterStart");
    }

    @Override
    public void beforeClaim(ClaimContext ctx) {
        log.debug("测试hook-beforeClaim");
    }

    @Override
    public void afterClaim(ClaimContext ctx) {
        log.debug("测试hook-afterClaim");
    }

    @Override
    public void beforePass(PassContext ctx) {
        log.debug("测试hook-beforePass");
    }

    @Override
    public void afterPass(PassContext ctx) {
        log.debug("测试hook-afterPass");
    }

    @Override
    public void beforeReject(RejectContext ctx) {
        log.debug("测试hook-beforeReject");
    }

    @Override
    public void afterReject(RejectContext ctx) {
        log.debug("测试hook-afterReject");
    }

    @Override
    public void beforeBack(BackContext ctx) {
        log.debug("测试hook-beforeBack");
    }

    @Override
    public void afterBack(BackContext ctx) {
        log.debug("测试hook-afterBack");
    }
}
