package com.cat.simple.config.flowable.command;

import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.enums.ProcessStatusEnum;
import com.cat.simple.config.flowable.hook.RejectContext;
import jakarta.annotation.Resource;
import org.flowable.engine.RuntimeService;
import org.flowable.task.api.Task;
import java.time.LocalDateTime;

public class RejectTaskCommand extends ProcessCommand<Void> {

    @Resource private RuntimeService runtimeService;
    @Resource private com.cat.simple.mapper.ProcessInstanceMapper processInstanceMapper;

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
    protected Void doExecute() {
        ProcessInstance instance = guard.getInstance(param.getProcessInstanceId());
        this.task = guard.getTask(param.getTaskId());
        String remark = param.getRemark() != null && !param.getRemark().isBlank()
                ? param.getRemark() : "拒绝";
        runtimeService.deleteProcessInstance(instance.getProcessInstanceId(), remark);
        return null;
    }

    @Override
    protected void record(Void result) {
        recorder.recordReject(param, task);
    }

    @Override
    protected void beforeHook() {
        RejectContext ctx = new RejectContext(param.getProcessInstanceId(), param.getTaskId(), param.getRemark());
        lifecycleHook.beforeReject(ctx);
    }

    @Override
    protected void afterHook(Void result) {
        ProcessInstance instance = guard.getInstance(param.getProcessInstanceId());
        processInstanceMapper.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProcessInstance>()
                .eq(ProcessInstance::getId, instance.getId())
                .set(ProcessInstance::getProcessStatus, ProcessStatusEnum.TERMINATED.getStatus())
                .set(ProcessInstance::getUpdateTime, LocalDateTime.now()));
        lifecycleHook.afterReject(instance);
    }
}
