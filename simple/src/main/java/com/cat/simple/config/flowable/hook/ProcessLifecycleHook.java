package com.cat.simple.config.flowable.hook;

import com.cat.common.entity.process.ProcessInstance;
import org.flowable.task.api.Task;

/**
 * 流程生命周期钩子接口，在关键操作前后提供扩展点。
 */
public interface ProcessLifecycleHook {

    default void beforeStart(StartContext ctx) { }
    default void afterStart(ProcessInstance instance) { }

    default void beforeClaim(ClaimContext ctx) { }
    default void afterClaim(ProcessInstance instance, Task task) { }

    default void beforePass(PassContext ctx) { }
    default void afterPass(ProcessInstance instance) { }

    default void beforeReject(RejectContext ctx) { }
    default void afterReject(ProcessInstance instance) { }

    default void beforeBack(BackContext ctx) { }
    default void afterBack(ProcessInstance instance, String targetNodeId) { }
}
