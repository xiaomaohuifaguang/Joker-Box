package com.cat.simple.config.flowable.command;

import com.cat.common.entity.auth.User;
import com.cat.common.entity.process.NextUserTaskInfo;
import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.approval.ApprovalContext;
import com.cat.simple.config.flowable.approval.ApprovalTypeEnum;
import com.cat.simple.config.flowable.candidate.CandidateResolver;
import com.cat.simple.config.flowable.enums.ProcessStatusEnum;
import com.cat.simple.config.flowable.gateway.GatewayConditionEngine;
import com.cat.simple.config.flowable.hook.ProcessLifecycleHook;
import com.cat.simple.config.flowable.hook.context.StartContext;
import com.cat.simple.config.flowable.util.FlowableUtils;
import com.cat.simple.config.process.ProcessCodeGenerator;
import com.cat.simple.process.service.ProcessFormService;
import jakarta.annotation.Resource;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 启动流程命令，根据流程定义创建新的流程实例。
 */
public class StartProcessCommand extends ProcessCommand<ProcessInstance> {

    @Resource private RuntimeService runtimeService;
    @Resource private TaskService taskService;
    @Resource private ProcessCodeGenerator codeGenerator;
    @Resource private com.cat.simple.process.mapper.ProcessInstanceMapper processInstanceMapper;
    @Resource private ProcessFormService processFormService;
    @Resource private com.cat.simple.process.service.ProcessDefinitionService processDefinitionService;
    @Resource private RepositoryService repositoryService;
    @Resource private GatewayConditionEngine gatewayConditionEngine;
    @Resource private FlowableUtils flowableUtils;
    @Resource private CandidateResolver candidateResolver;

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

        Map<String, Object> variables = new HashMap<>();

        Map<String, List<Integer>> nodeCandidateUsersChoose = param.getNodeCandidateUsersChoose();

        List<UserTask> startNextUserTasksSkipGateway = flowableUtils.findStartNextUserTasksSkipGateway(definition.getProcessKey(), instance.getProcessDefinitionVersion());

        for (UserTask userTask : startNextUserTasksSkipGateway) {
            ApprovalContext ctx = ApprovalContext.from(userTask);
            if(ctx.type().equals(ApprovalTypeEnum.CHOOSE) || ctx.type().equals(ApprovalTypeEnum.CHOOSE_COUNTERSIGN) || ctx.type().equals(ApprovalTypeEnum.CHOOSE_OR_SIGN)){
                if(Objects.isNull(nodeCandidateUsersChoose)){
                    throw new IllegalStateException("请选择处理人");
                }
                List<Integer> chooseUsers = nodeCandidateUsersChoose.get(userTask.getId());
                List<Integer> chooseUsersFilterNull = chooseUsers.stream()
                        .filter(Objects::nonNull)
                        .toList();
                if(CollectionUtils.isEmpty(chooseUsersFilterNull)){
                    throw new IllegalStateException("请选择合适的处理人");
                }
                if(ctx.type().equals(ApprovalTypeEnum.CHOOSE) && chooseUsersFilterNull.size() > 1){
                    throw new IllegalStateException("请选择合适的处理人");
                }
                LinkedHashSet<User> usersByCtxWithoutApplicant = candidateResolver.getUsersByCtxWithoutApplicant(ctx);
                List<Integer>  candidateUsers = usersByCtxWithoutApplicant.stream().map(User::getId).toList();
                boolean hasInvalid = chooseUsersFilterNull.stream()
                        .anyMatch(user -> !candidateUsers.contains(user));
                if(hasInvalid){
                    throw new IllegalStateException("请选择合适的处理人");
                }
                variables.put("choose_"+userTask.getId(), chooseUsersFilterNull);
            }
        }


        org.flowable.engine.runtime.ProcessInstance flowableInstance =
                runtimeService.startProcessInstanceByKey(definition.getProcessKey(), String.valueOf(instance.getId()), variables);

        instance.setProcessInstanceId(flowableInstance.getProcessInstanceId());

        processInstanceMapper.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProcessInstance>()
                .eq(ProcessInstance::getId, instance.getId())
                .set(ProcessInstance::getProcessDefinitionId, definition.getId())
                .set(ProcessInstance::getTitle, param.getTitle())
                .set(ProcessInstance::getProcessInstanceId, instance.getProcessInstanceId())
                .set(ProcessInstance::getProcessStatus, ProcessStatusEnum.ACTIVE.getStatus())
                .set(ProcessInstance::getUpdateTime, now));

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

        List<Task> list = taskService.createTaskQuery().processInstanceId(result.getProcessInstanceId()).list();
        if(!CollectionUtils.isEmpty(list) && list.size() == 1 &&  list.get(0).getTaskDefinitionKey().equals("applyNode")){
            Task task = list.get(0);

            taskService.complete(task.getId());
        }


    }


}