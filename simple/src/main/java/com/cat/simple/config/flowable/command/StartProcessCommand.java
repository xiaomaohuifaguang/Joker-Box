package com.cat.simple.config.flowable.command;

import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.enums.ProcessStatusEnum;
import com.cat.simple.config.process.ProcessCodeGenerator;
import jakarta.annotation.Resource;
import org.flowable.engine.RuntimeService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class StartProcessCommand extends ProcessCommand<ProcessInstance> {

    @Resource private RuntimeService runtimeService;
    @Resource private ProcessCodeGenerator codeGenerator;
    @Resource private com.cat.simple.mapper.ProcessInstanceMapper processInstanceMapper;

    private final Integer processDefinitionId;
    private final String title;

    public StartProcessCommand(Integer processDefinitionId, String title) {
        this.processDefinitionId = processDefinitionId;
        this.title = title;
    }

    @Override
    protected void validate() {
        guard.assertDefinitionPublished(processDefinitionId);
    }

    @Override
    protected ProcessInstance doExecute() {
        com.cat.common.entity.process.ProcessDefinition definition =
                guard.assertDefinitionPublished(processDefinitionId);
        String currentUserId = guard.getCurrentUserId();

        org.flowable.engine.runtime.ProcessInstance flowableInstance =
                runtimeService.startProcessInstanceByKey(definition.getProcessKey());

        LocalDateTime now = LocalDateTime.now();
        ProcessInstance instance = new ProcessInstance()
                .setProcessDefinitionId(definition.getId())
                .setTitle(title)
                .setCode(codeGenerator.generate())
                .setProcessInstanceId(flowableInstance.getProcessInstanceId())
                .setProcessStatus(ProcessStatusEnum.ACTIVE.getStatus())
                .setCreateBy(currentUserId)
                .setCreateTime(now)
                .setUpdateTime(now);
        processInstanceMapper.insert(instance);

        // 兜底：trivial 流程立即结束
        if (runtimeService.createProcessInstanceQuery()
                .processInstanceId(flowableInstance.getProcessInstanceId())
                .singleResult() == null) {
            processInstanceMapper.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProcessInstance>()
                    .eq(ProcessInstance::getId, instance.getId())
                    .set(ProcessInstance::getProcessStatus, ProcessStatusEnum.COMPLETED.getStatus())
                    .set(ProcessInstance::getUpdateTime, now));
        }

        return instance;
    }

    @Override
    protected void record(ProcessInstance result) {
        recorder.recordApply(result, guard.getCurrentUserId());
    }
}
