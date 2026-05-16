package com.cat.simple.config.flowable.command;

import com.cat.common.entity.process.ProcessHandleParam;
import jakarta.annotation.Resource;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

@Component
public class PassTaskCommand extends ProcessCommand<Void> {

    @Resource private TaskService taskService;

    private final ProcessHandleParam param;

    public PassTaskCommand(ProcessHandleParam param) {
        this.param = param;
    }

    @Override
    protected void validate() {
        guard.assertInstanceActive(param.getProcessInstanceId());
        guard.assertTaskAssignee(param.getTaskId());
    }

    @Override
    protected Void doExecute() {
        Task task = guard.getTask(param.getTaskId());
        taskService.complete(task.getId());
        return null;
    }

    @Override
    protected void record(Void result) {
        Task task = guard.getTask(param.getTaskId());
        recorder.recordPass(param, task);
    }
}
