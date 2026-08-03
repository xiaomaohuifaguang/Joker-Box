package com.cat.simple.config.flowable.hook;

import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.hook.context.*;
import org.flowable.task.api.Task;

/**
 * 流程生命周期钩子接口，在关键操作前后提供扩展点。
 */
public interface ProcessLifecycleHook {

    default void beforeStart(StartContext ctx) { }
    default void afterStart(StartContext ctx) { }

    default void beforeClaim(ClaimContext ctx) { }
    default void afterClaim(ClaimContext ctx) { }

    default void beforePass(PassContext ctx) { }
    default void afterPass(PassContext ctx) { }

    default void beforeReject(RejectContext ctx) { }
    default void afterReject(RejectContext ctx) { }

    default void beforeBack(BackContext ctx) { }
    default void afterBack(BackContext ctx) { }
}
