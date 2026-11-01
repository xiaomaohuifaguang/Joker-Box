package com.cat.simple.config.flowable.command;

import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.core.CatTaskInfo;
import com.cat.simple.config.flowable.enums.ProcessStatusEnum;
import com.cat.simple.config.flowable.hook.ProcessLifecycleHook;
import com.cat.simple.config.flowable.hook.context.RejectContext;
import jakarta.annotation.Resource;
import org.flowable.engine.RuntimeService;
import org.flowable.task.api.Task;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;

/**
 * 拒绝任务命令，终止当前流程实例。
 */
public class RejectTaskCommand extends ProcessCommand<CatTaskInfo> {

    @Resource private RuntimeService runtimeService;
    private final ProcessHandleParam param;
    private Task task;

    public RejectTaskCommand(ProcessHandleParam param) {
        this.param = param;
    }

    @Override
    protected void validate() {
        guard.assertInstanceActive(param.getProcessInstanceId());
        guard.assertTaskAssignee(param.getTaskId());
    }

    @Override
    protected CatTaskInfo doExecute() {
        ProcessInstance instance = guard.getInstance(param.getProcessInstanceId());
        this.task = guard.getTask(param.getTaskId());
        CatTaskInfo taskInfo = guard.toTaskInfo(task);
        String remark = param.getRemark() != null && !param.getRemark().isBlank()
                ? param.getRemark() : "拒绝";
        runtimeService.deleteProcessInstance(instance.getProcessInstanceId(), remark);
        return taskInfo;
    }

    @Override
    protected void record(CatTaskInfo result) {
        recorder.recordReject(param, task,result);
    }

    @Override
    protected void beforeHook() {
        RejectContext ctx = new RejectContext(param);
        if (!CollectionUtils.isEmpty(lifecycleHooks)) {
            for (ProcessLifecycleHook hook : lifecycleHooks) {
                hook.beforeReject(ctx);
            }
        }
    }

    @Override
    protected void afterHook(CatTaskInfo result) {
        RejectContext ctx = new RejectContext(param);
        if (!CollectionUtils.isEmpty(lifecycleHooks)) {
            for (ProcessLifecycleHook hook : lifecycleHooks) {
                hook.afterReject(ctx);
            }
        }
    }
}
