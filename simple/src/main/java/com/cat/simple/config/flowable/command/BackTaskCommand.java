package com.cat.simple.config.flowable.command;

import com.cat.common.entity.process.BackConfig;
import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.simple.config.flowable.core.CatTaskInfo;
import com.cat.simple.config.flowable.engine.BackEngine;
import com.cat.simple.config.flowable.hook.ProcessLifecycleHook;
import com.cat.simple.config.flowable.hook.context.BackContext;
import com.cat.simple.config.flowable.util.FlowableUtils;
import jakarta.annotation.Resource;
import org.flowable.task.api.Task;
import org.springframework.util.CollectionUtils;

/**
 * 驳回任务命令，将当前任务回退到指定或上一节点。
 */
public class BackTaskCommand extends ProcessCommand<CatTaskInfo> {


    @Resource private FlowableUtils flowableUtils;
    @Resource private BackEngine backEngine;

    private final ProcessHandleParam param;
    private Task task;

    public BackTaskCommand(ProcessHandleParam param) {
        this.param = param;
    }

    @Override
    protected void validate() {
        guard.assertInstanceActive(param.getProcessInstanceId());
        guard.assertTaskAssignee(param.getTaskId());
    }

    @Override
    protected CatTaskInfo doExecute() {


        task = guard.getTask(param.getTaskId());
        CatTaskInfo taskInfo = guard.toTaskInfo(task);

        backEngine.back(param, task);

        return taskInfo;
    }

    @Override
    protected void record(CatTaskInfo result) {
        recorder.recordBack(param, task,result);
    }

    @Override
    protected void beforeHook() {
        BackContext ctx = new BackContext(param);
        if (!CollectionUtils.isEmpty(lifecycleHooks)) {
            for (ProcessLifecycleHook hook : lifecycleHooks) {
                hook.beforeBack(ctx);
            }
        }
    }

    @Override
    protected void afterHook(CatTaskInfo result) {
        BackContext ctx = new BackContext(param);
        if (!CollectionUtils.isEmpty(lifecycleHooks)) {
            for (ProcessLifecycleHook hook : lifecycleHooks) {
                hook.afterBack(ctx);
            }
        }
    }



}
