package com.cat.simple.config.flowable.command;

import com.cat.common.entity.auth.User;
import com.cat.common.entity.process.ProcessDefinition;
import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.approval.ApprovalContext;
import com.cat.simple.config.flowable.approval.ApprovalTypeEnum;
import com.cat.simple.config.flowable.candidate.CandidateResolver;
import com.cat.simple.config.flowable.hook.ProcessLifecycleHook;
import com.cat.simple.config.flowable.hook.context.PassContext;
import com.cat.simple.config.flowable.util.FlowableUtils;
import com.cat.simple.process.service.ProcessFormService;
import jakarta.annotation.Resource;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static com.cat.common.entity.process.constants.VariablesConstants.CHOOSE_PRE;


public class PassTaskCommand extends ProcessCommand<Void> {

    @Resource private TaskService taskService;
    @Resource private ProcessFormService processFormService;
    @Resource private FlowableUtils flowableUtils;
    @Resource private CandidateResolver candidateResolver;

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
        ProcessInstance instance = guard.getInstance(param.getProcessInstanceId());
        ProcessDefinition definition = guard.assertDefinitionExistAndNoDraft(instance.getProcessDefinitionId());
        processFormService.writeFormData(instance, param.getGlobalFormData());

        // 校验并写入表单数据
//        boolean hasNodeData = param.getNodeFormData() != null && !param.getNodeFormData().isEmpty();
//        boolean hasGlobalData = param.getGlobalFormData() != null && !param.getGlobalFormData().isEmpty();
//        if (hasNodeData || hasGlobalData) {
//            processFormService.writeFormData(
//                    param.getProcessInstanceId(),
//                    instance.getProcessDefinitionId(),
//                    instance.getProcessDefinitionVersion(),
//                    task.getTaskDefinitionKey(),
//                    param.getNodeFormData(),
//                    param.getGlobalFormData(),
//                    false);
//        }

        // 注入表单字段为流程变量，供 NATIVE JUEL 表达式使用
//        Map<String, Object> allFormData = new HashMap<>();
//        if (param.getNodeFormData() != null) allFormData.putAll(param.getNodeFormData());
//        if (param.getGlobalFormData() != null) allFormData.putAll(param.getGlobalFormData());
//
//        for (Map.Entry<String, Object> entry : allFormData.entrySet()) {
//            variableStore.setRaw(instance.getProcessInstanceId(), entry.getKey(), entry.getValue());
//        }
//        // 同时注入 formData Map，支持 ${formData['amount']}
//        variableStore.set(instance.getProcessInstanceId(), VariableNames.FORM_DATA, allFormData);
//
//        // 注入内置变量（字符串类型）
//        LoginUser loginUser = SecurityUtils.getLoginUser();
//        if (loginUser != null) {
//            // handler_dept: List<String>
//            if (loginUser.getOrgs() != null) {
//                List<String> deptIds = loginUser.getOrgs().stream()
//                        .map(org -> String.valueOf(org.getId()))
//                        .collect(Collectors.toList());
//                variableStore.set(instance.getProcessInstanceId(), VariableNames.HANDLER_DEPT, deptIds);
//            }
//            // handler_role: List<String>
//            if (loginUser.getRoles() != null) {
//                List<String> roleIds = loginUser.getRoles().stream()
//                        .map(role -> String.valueOf(role.getId()))
//                        .collect(Collectors.toList());
//                variableStore.set(instance.getProcessInstanceId(), VariableNames.HANDLER_ROLE, roleIds);
//            }
//        }

        // 网关 CUSTOM 条件由 GatewayConditionEvaluator 在 Flowable 评估出线时懒加载计算
        // 这里不再预计算，避免错误地以当前任务节点作为 sourceNodeId 导致漏算

        Map<String, Object> variables = new HashMap<>();

        Map<String, List<Integer>> nodeCandidateUsersChoose = param.getNodeCandidateUsersChoose();

        List<UserTask> nextUserTasksSkipGateway = flowableUtils.findNextUserTasksSkipGateway(definition.getProcessKey(), instance.getProcessDefinitionVersion(), task.getTaskDefinitionKey());

        for (UserTask userTask : nextUserTasksSkipGateway) {
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
                variables.put(CHOOSE_PRE+userTask.getId(), chooseUsersFilterNull);
            }
        }

        taskService.complete(task.getId(), variables);
        return null;
    }

    @Override
    protected void record(Void result) {
        recorder.recordPass(param, task);
    }

    @Override
    protected void beforeHook() {
        PassContext ctx = new PassContext(param);
        if (!CollectionUtils.isEmpty(lifecycleHooks)) {
            for (ProcessLifecycleHook hook : lifecycleHooks) {
                hook.beforePass(ctx);
            }
        }
    }

    @Override
    protected void afterHook(Void result) {
        PassContext ctx = new PassContext(param);
        if (!CollectionUtils.isEmpty(lifecycleHooks)) {
            for (ProcessLifecycleHook hook : lifecycleHooks) {
                hook.afterPass(ctx);
            }
        }
    }
}