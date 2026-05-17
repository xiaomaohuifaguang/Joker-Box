package com.cat.simple.config.flowable.command;

import com.cat.simple.config.flowable.guard.ProcessGuard;
import com.cat.simple.config.flowable.hook.ProcessLifecycleHook;
import com.cat.simple.config.flowable.recorder.HandleInfoRecorder;
import com.cat.simple.config.flowable.variable.ProcessVariableStore;
import jakarta.annotation.Resource;

/**
 * 流程命令抽象基类，定义命令执行的标准模板：
 * validate → beforeHook → doExecute → afterHook → record。
 * 所有具体流程操作（启动、认领、通过、驳回等）均继承此类实现。
 */
public abstract class ProcessCommand<T> {

    @Resource protected ProcessGuard guard;
    @Resource protected HandleInfoRecorder recorder;
    @Resource protected ProcessLifecycleHook lifecycleHook;
    @Resource protected ProcessVariableStore variableStore;

    /**
     * 执行命令模板，按固定顺序调用各阶段方法。
     */
    public final T execute() {
        validate();
        beforeHook();
        T result = doExecute();
        afterHook(result);
        record(result);
        return result;
    }

    /**
     * 校验阶段：验证流程实例、任务、定义等前置条件。
     */
    protected abstract void validate();

    /**
     * 执行阶段：调用 Flowable API 完成实际业务操作。
     */
    protected abstract T doExecute();

    /**
     * 记录阶段：将操作结果持久化为 ProcessHandleInfo。
     */
    protected abstract void record(T result);

    /**
     * 执行前钩子，子类可覆盖以扩展前置逻辑。
     */
    protected void beforeHook() { }

    /**
     * 执行后钩子，子类可覆盖以扩展后置逻辑。
     */
    protected void afterHook(T result) { }
}
