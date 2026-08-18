package com.cat.simple.config.flowable.command;

import com.cat.common.entity.process.ProcessInstance;
import jakarta.annotation.Resource;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;

/**
 * 自动通过命令（autoApproveIfSelf=1 且任务 assignee 与申请人一致时触发）。
 * 由 ApprovalTaskCreateListener 在事务提交后异步触发，无登录上下文，
 * 因此不复用 PassTaskCommand（其 assignee 校验与记录均依赖 SecurityUtils）。
 */
public class AutoPassTaskCommand extends ProcessCommand<Void> {

    @Resource private TaskService taskService;
    @Resource private RuntimeService runtimeService;

    private final String taskId;
    private Task task;
    private ProcessInstance instance;

    public AutoPassTaskCommand(String taskId) {
        this.taskId = taskId;
    }

    @Override
    protected void validate() {
        this.task = guard.assertTaskExists(taskId);
        org.flowable.engine.runtime.ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
        this.instance = guard.selectByFlowableBusinessKey(processInstance.getBusinessKey());
        if (instance == null) {
            throw new IllegalStateException("业务流程实例不存在, flowableProcessInstanceId: " + task.getProcessInstanceId());
        }
//        guard.assertInstanceActive(instance.getId());
    }

    @Override
    protected Void doExecute() {
        taskService.complete(taskId);
        return null;
    }

    @Override
    protected void record(Void result) {
        recorder.recordAutoPass(instance, task);
    }
}
