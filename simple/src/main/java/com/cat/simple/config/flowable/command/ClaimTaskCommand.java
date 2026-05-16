package com.cat.simple.config.flowable.command;

import com.cat.common.entity.process.ProcessHandleParam;
import jakarta.annotation.Resource;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

@Component
public class ClaimTaskCommand extends ProcessCommand<Void> {

    @Resource private TaskService taskService;

    private final ProcessHandleParam param;

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
        Task task = guard.getTask(param.getTaskId());
        taskService.claim(param.getTaskId(), guard.getCurrentUserId());
        return null;
    }

    @Override
    protected void record(Void result) {
        Task task = guard.getTask(param.getTaskId());
        recorder.recordClaim(param, task);
    }
}
