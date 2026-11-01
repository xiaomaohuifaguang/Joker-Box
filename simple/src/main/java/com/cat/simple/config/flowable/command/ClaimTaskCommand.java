package com.cat.simple.config.flowable.command;

import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.simple.config.flowable.core.CatTaskInfo;
import com.cat.simple.config.flowable.hook.ProcessLifecycleHook;
import com.cat.simple.config.flowable.hook.context.ClaimContext;
import jakarta.annotation.Resource;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.util.CollectionUtils;

/**
 * 认领任务命令，当前候选人将任务指派给自己。
 */
public class ClaimTaskCommand extends ProcessCommand<CatTaskInfo> {

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
    protected CatTaskInfo doExecute() {
        taskService.claim(param.getTaskId(), guard.getCurrentUserId());
        this.task = guard.getTask(param.getTaskId());
        return guard.toTaskInfo(task);
    }

    @Override
    protected void record(CatTaskInfo result) {
        recorder.recordClaim(param, task,result);
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
    protected void afterHook(CatTaskInfo result) {
        ClaimContext ctx = new ClaimContext(param);
        if (!CollectionUtils.isEmpty(lifecycleHooks)) {
            for (ProcessLifecycleHook hook : lifecycleHooks) {
                hook.afterClaim(ctx);
            }
        }
    }
}
