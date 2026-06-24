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
import java.util.Objects;

/**
 * 启动流程命令，根据流程定义创建新的流程实例。
 */
public class StartProcessCommand extends ProcessCommand<ProcessInstance> {

    @Resource private RuntimeService runtimeService;
    @Resource private ProcessCodeGenerator codeGenerator;
    @Resource private com.cat.simple.process.mapper.ProcessInstanceMapper processInstanceMapper;
    @Resource private ProcessFormService processFormService;
    @Resource private com.cat.simple.process.service.ProcessDefinitionService processDefinitionService;

    private final Integer processInstanceId;
    private final Integer processDefinitionId;
    private final String title;
    private final Map<String, Object> nodeFormData;
    private final Map<String, Object> globalFormData;

    public StartProcessCommand(Integer processDefinitionId, Integer processInstanceId, String title,
                               Map<String, Object> nodeFormData, Map<String, Object> globalFormData) {
        this.processDefinitionId = processDefinitionId;
        this.processInstanceId = processInstanceId;
        this.title = title;
        this.nodeFormData = nodeFormData;
        this.globalFormData = globalFormData;
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

        ProcessInstance instance;
        if (Objects.nonNull(processInstanceId)) {
            // 传了流程实例ID → 草稿发起：校验存在且为草稿
            instance = guard.assertInstanceDraft(processInstanceId);
            if (!currentUserId.equals(instance.getCreateBy())) {
                throw new IllegalStateException("无权操作他人草稿: " + processInstanceId);
            }

            // 更新草稿实例的状态和字段
            processInstanceMapper.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProcessInstance>()
                    .eq(ProcessInstance::getId, instance.getId())
                    .set(ProcessInstance::getProcessDefinitionId, definition.getId())
//                    .set(ProcessInstance::getProcessDefinitionVersion, definition.getVersion())
                    .set(ProcessInstance::getTitle, title)
                    .set(ProcessInstance::getProcessInstanceId, flowableInstance.getProcessInstanceId())
                    .set(ProcessInstance::getProcessStatus, ProcessStatusEnum.ACTIVE.getStatus())
                    .set(ProcessInstance::getCode, codeGenerator.generate())
                    .set(ProcessInstance::getUpdateTime, now));

            instance.setProcessDefinitionId(definition.getId())
                    .setProcessDefinitionVersion(definition.getVersion())
                    .setTitle(title)
                    .setCode(codeGenerator.generate())
                    .setProcessInstanceId(flowableInstance.getProcessInstanceId())
                    .setProcessStatus(ProcessStatusEnum.ACTIVE.getStatus())
                    .setUpdateTime(now);
        } else {
            instance = new ProcessInstance()
                    .setProcessDefinitionId(definition.getId())
                    .setProcessDefinitionVersion(definition.getVersion())
                    .setTitle(title)
                    .setCode(codeGenerator.generate())
                    .setProcessInstanceId(flowableInstance.getProcessInstanceId())
                    .setProcessStatus(ProcessStatusEnum.ACTIVE.getStatus())
                    .setCreateBy(currentUserId)
                    .setCreateTime(now)
                    .setUpdateTime(now);
            processInstanceMapper.insert(instance);
        }

        // 创建表单实例并写入数据
        String startNodeId = processDefinitionService.resolveStartEventNodeId(processDefinitionId);
        processFormService.createFormInstanceIfNeeded(instance.getId(), definition.getId(),
                definition.getVersion(), startNodeId);
        processFormService.writeFormData(instance.getId(), definition.getId(),
                definition.getVersion(), startNodeId,
                nodeFormData, globalFormData, false);

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
        StartContext ctx = new StartContext(processDefinitionId, title, guard.getCurrentUserId(), null, nodeFormData, globalFormData);
        lifecycleHook.beforeStart(ctx);
    }

    @Override
    protected void afterHook(ProcessInstance result) {
        lifecycleHook.afterStart(result);
    }
}