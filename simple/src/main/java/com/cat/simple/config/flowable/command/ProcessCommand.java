package com.cat.simple.config.flowable.command;

import com.cat.simple.config.flowable.guard.ProcessGuard;
import com.cat.simple.config.flowable.hook.ProcessLifecycleHook;
import com.cat.simple.config.flowable.recorder.HandleInfoRecorder;
import com.cat.simple.config.flowable.variable.ProcessVariableStore;
import jakarta.annotation.Resource;

public abstract class ProcessCommand<T> {

    @Resource protected ProcessGuard guard;
    @Resource protected HandleInfoRecorder recorder;
    @Resource protected ProcessLifecycleHook lifecycleHook;
    @Resource protected ProcessVariableStore variableStore;

    public final T execute() {
        validate();
        beforeHook();
        T result = doExecute();
        afterHook(result);
        record(result);
        return result;
    }

    protected abstract void validate();
    protected abstract T doExecute();
    protected abstract void record(T result);

    protected void beforeHook() { }
    protected void afterHook(T result) { }
}
