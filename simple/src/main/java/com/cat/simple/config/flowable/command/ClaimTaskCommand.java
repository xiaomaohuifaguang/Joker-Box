package com.cat.simple.config.flowable.command;

import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.simple.config.flowable.hook.ProcessLifecycleHook;
import com.cat.simple.config.flowable.hook.context.ClaimContext;
import jakarta.annotation.Resource;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.util.CollectionUtils;

/**
 * 认领任务命令，当前候选人将任务指派给自己。
 */
public class ClaimTaskCommand extends ProcessCommand<Void> {

    @Resource private TaskService taskService;

    private final ProcessHandleParam param;
    private Task task;

    public ClaimTaskCommand(ProcessHandleParam param) {
        this.param = param;
    }

    @Override
    protected void validate() {
        guard.assertInstanceActive(param.getProcessInstanceId());
        guard.assertTaskCandidate(param.getTaskId());
    }

    @Override
    protected Void doExecute() {
        taskService.claim(param.getTaskId(), guard.getCurrentUserId());
        this.task = guard.getTask(param.getTaskId());
        return null;
    }

    @Override
    protected void record(Void result) {
        recorder.recordClaim(param, task);
    }

    @Override
    protected void beforeHook() {
        ClaimContext ctx = new ClaimContext(param);
        if (!CollectionUtils.isEmpty(lifecycleHooks)) {
            for (ProcessLifecycleHook hook : lifecycleHooks) {
                hook.beforeClaim(ctx);
            }
        }
    }

    @Override
    protected void afterHook(Void result) {
        ClaimContext ctx = new ClaimContext(param);
        if (!CollectionUtils.isEmpty(lifecycleHooks)) {
            for (ProcessLifecycleHook hook : lifecycleHooks) {
                hook.afterClaim(ctx);
            }
        }
    }
}
