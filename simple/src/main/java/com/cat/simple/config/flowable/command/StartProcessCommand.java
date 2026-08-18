package com.cat.simple.config.flowable.command;

import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.enums.ProcessStatusEnum;
import com.cat.simple.config.flowable.hook.ProcessLifecycleHook;
import com.cat.simple.config.flowable.hook.context.StartContext;
import com.cat.simple.config.process.ProcessCodeGenerator;
import com.cat.simple.process.service.ProcessFormService;
import jakarta.annotation.Resource;
import org.flowable.engine.RuntimeService;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
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

    private final ProcessHandleParam param;

    public StartProcessCommand(ProcessHandleParam param) {
        this.param = param;
    }

    @Override
    protected void validate() {
        guard.assertDefinitionPublished(param.getProcessDefinitionId());
    }

    @Override
    protected ProcessInstance doExecute() {
        com.cat.common.entity.process.ProcessDefinition definition =
                guard.assertDefinitionPublished(param.getProcessDefinitionId());
        String currentUserId = guard.getCurrentUserId();



        LocalDateTime now = LocalDateTime.now();

        ProcessInstance instance;
        if (Objects.nonNull(param.getProcessInstanceId())) {
            // 传了流程实例ID → 草稿发起：校验存在且为草稿
            instance = guard.assertInstanceDraft(param.getProcessInstanceId());
            if (!currentUserId.equals(instance.getCreateBy())) {
                throw new IllegalStateException("无权操作他人草稿: " + param.getProcessInstanceId());
            }
            instance.setTitle(param.getTitle())
                    .setCode(codeGenerator.generate())
                    .setUpdateTime(now);
            processInstanceMapper.updateById(instance);

            processFormService.writeFormData(instance, param.getGlobalFormData());

        } else {

            instance = new ProcessInstance()
                    .setProcessDefinitionId(definition.getId())
                    .setProcessDefinitionVersion(definition.getVersion())
                    .setTitle(param.getTitle())
                    .setCode(codeGenerator.generate())
                    .setProcessStatus(ProcessStatusEnum.DRAFT.getStatus())
                    .setCreateBy(currentUserId)
                    .setCreateTime(now)
                    .setUpdateTime(now);
            processInstanceMapper.insert(instance);
            processFormService.writeFormData(instance, param.getGlobalFormData());

        }

        org.flowable.engine.runtime.ProcessInstance flowableInstance =
                runtimeService.startProcessInstanceByKey(definition.getProcessKey(), String.valueOf(instance.getId()));
        instance.setProcessInstanceId(flowableInstance.getProcessInstanceId());

        processInstanceMapper.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProcessInstance>()
                .eq(ProcessInstance::getId, instance.getId())
                .set(ProcessInstance::getProcessDefinitionId, definition.getId())
                .set(ProcessInstance::getTitle, param.getTitle())
                .set(ProcessInstance::getProcessInstanceId, instance.getProcessInstanceId())
                .set(ProcessInstance::getProcessStatus, ProcessStatusEnum.ACTIVE.getStatus())
                .set(ProcessInstance::getUpdateTime, now));

//        // 创建表单实例并写入数据
//        String startNodeId = processDefinitionService.resolveStartEventNodeId(param.getProcessDefinitionId());
//        processFormService.createFormInstanceIfNeeded(instance.getId(), definition.getId(),
//                definition.getVersion(), startNodeId);
//        processFormService.writeFormData(instance.getId(), definition.getId(),
//                definition.getVersion(), startNodeId,
//                param.getNodeFormData(), param.getGlobalFormData(), false);

//        processFormService.writeFormData(instance, param.getGlobalFormData());

        // 兜底：trivial 流程立即结束
        if (runtimeService.createProcessInstanceQuery()
                .processInstanceId(instance.getProcessInstanceId())
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
        StartContext ctx = new StartContext(param);
        if (!CollectionUtils.isEmpty(lifecycleHooks)) {
            for (ProcessLifecycleHook hook : lifecycleHooks) {
                hook.beforeStart(ctx);
            }
        }
    }

    @Override
    protected void afterHook(ProcessInstance result) {
        StartContext ctx = new StartContext(param);
        if (!CollectionUtils.isEmpty(lifecycleHooks)) {
            for (ProcessLifecycleHook hook : lifecycleHooks) {
                hook.afterStart(ctx);
            }
        }
    }
}