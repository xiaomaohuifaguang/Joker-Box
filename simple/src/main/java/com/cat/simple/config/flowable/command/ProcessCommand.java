package com.cat.simple.config.flowable.command;

import com.cat.simple.config.flowable.guard.ProcessGuard;
import com.cat.simple.config.flowable.hook.ProcessLifecycleHook;
import com.cat.simple.config.flowable.recorder.HandleInfoRecorder;
import jakarta.annotation.Resource;

import java.util.List;

/**
 * 流程命令抽象基类，定义命令执行的标准模板：
 * validate → beforeHook → doExecute → afterHook → record。
 * 所有具体流程操作（启动、认领、通过、驳回等）均继承此类实现。
 */
public abstract class ProcessCommand<T> {

    @Resource protected ProcessGuard guard;
    @Resource protected HandleInfoRecorder recorder;
    @Resource protected List<ProcessLifecycleHook> lifecycleHooks;

    /**
     * 执行命令模板，按固定顺序调用各阶段方法。
     */
    public final T execute() {
//        System.out.println("开始: "+ System.currentTimeMillis() + "ms");
//        long start = System.currentTimeMillis();
        validate();
//        System.out.println("validate: "+ (System.currentTimeMillis() - start) + "ms");
//        start = System.currentTimeMillis();
        beforeHook();
//        System.out.println("beforeHook: "+ (System.currentTimeMillis() - start) + "ms");
//        start = System.currentTimeMillis();
        T result = doExecute();
//        System.out.println("doExecute: "+ (System.currentTimeMillis() - start) + "ms");
//        start = System.currentTimeMillis();
        record(result);
//        System.out.println("record: "+ (System.currentTimeMillis() - start) + "ms");
//        start = System.currentTimeMillis();
        afterHook(result);
//        System.out.println("afterHook: "+ (System.currentTimeMillis() - start) + "ms");
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
