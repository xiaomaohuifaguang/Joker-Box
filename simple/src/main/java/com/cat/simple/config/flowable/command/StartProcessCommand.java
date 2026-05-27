package com.cat.simple.config.flowable.command;

import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.enums.ProcessStatusEnum;
import com.cat.simple.config.flowable.hook.StartContext;
import com.cat.simple.config.process.ProcessCodeGenerator;
import com.cat.simple.process.service.ProcessFormService;
import jakarta.annotation.Resource;
import org.flowable.engine.RuntimeService;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 启动流程命令，根据流程定义创建新的流程实例。
 */
public class StartProcessCommand extends ProcessCommand<ProcessInstance> {

    @Resource private RuntimeService runtimeService;
    @Resource private ProcessCodeGenerator codeGenerator;
    @Resource private com.cat.simple.process.mapper.ProcessInstanceMapper processInstanceMapper;
    @Resource private ProcessFormService processFormService;

    private final Integer processDefinitionId;
    private final String title;
    private final Map<String, Object> formData;

    public StartProcessCommand(Integer processDefinitionId, String title, Map<String, Object> formData) {
        this.processDefinitionId = processDefinitionId;
        this.title = title;
        this.formData = formData;
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

        // 创建表单实例并写入数据
        processFormService.createFormInstanceIfNeeded(instance.getId(), definition.getId(), null);
        processFormService.writeFormData(instance.getId(), definition.getId(), null, formData, false);

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

    @Override
    protected void beforeHook() {
        StartContext ctx = new StartContext(processDefinitionId, title, guard.getCurrentUserId(), null, formData);
        lifecycleHook.beforeStart(ctx);
    }

    @Override
    protected void afterHook(ProcessInstance result) {
        lifecycleHook.afterStart(result);
    }
}