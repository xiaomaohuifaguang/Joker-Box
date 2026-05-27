package com.cat.simple.config.flowable.command;

import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.hook.PassContext;
import com.cat.simple.process.service.ProcessFormService;
import jakarta.annotation.Resource;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;

public class PassTaskCommand extends ProcessCommand<Void> {

    @Resource private TaskService taskService;
    @Resource private ProcessFormService processFormService;

    private final ProcessHandleParam param;
    private Task task;

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
        this.task = guard.getTask(param.getTaskId());

        // 校验并写入表单数据
        if (param.getFormData() != null && !param.getFormData().isEmpty()) {
            ProcessInstance instance = guard.getInstance(param.getProcessInstanceId());
            processFormService.writeFormData(
                    param.getProcessInstanceId(),
                    instance.getProcessDefinitionId(),
                    task.getTaskDefinitionKey(),
                    param.getFormData(),
                    false);
        }

        taskService.complete(task.getId());
        return null;
    }

    @Override
    protected void record(Void result) {
        recorder.recordPass(param, task);
    }

    @Override
    protected void beforeHook() {
        PassContext ctx = new PassContext(param.getProcessInstanceId(), param.getTaskId(),
                param.getRemark(), param.getFormData());
        lifecycleHook.beforePass(ctx);
    }

    @Override
    protected void afterHook(Void result) {
        lifecycleHook.afterPass(guard.getInstance(param.getProcessInstanceId()));
    }
}