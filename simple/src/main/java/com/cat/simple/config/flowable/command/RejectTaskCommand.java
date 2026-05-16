package com.cat.simple.config.flowable.command;

import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.enums.ProcessStatusEnum;
import jakarta.annotation.Resource;
import org.flowable.engine.RuntimeService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RejectTaskCommand extends ProcessCommand<Void> {

    @Resource private RuntimeService runtimeService;
    @Resource private com.cat.simple.mapper.ProcessInstanceMapper processInstanceMapper;

    private final ProcessHandleParam param;

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
        Task task = guard.getTask(param.getTaskId());
        String remark = param.getRemark() != null && !param.getRemark().isBlank()
                ? param.getRemark() : "拒绝";
        runtimeService.deleteProcessInstance(instance.getProcessInstanceId(), remark);
        return null;
    }

    @Override
    protected void record(Void result) {
        Task task = guard.getTask(param.getTaskId());
        recorder.recordReject(param, task);
    }

    @Override
    protected void afterHook(Void result) {
        ProcessInstance instance = guard.getInstance(param.getProcessInstanceId());
        processInstanceMapper.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProcessInstance>()
                .eq(ProcessInstance::getId, instance.getId())
                .set(ProcessInstance::getProcessStatus, ProcessStatusEnum.TERMINATED.getStatus())
                .set(ProcessInstance::getUpdateTime, LocalDateTime.now()));
    }
}
