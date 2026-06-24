package com.cat.simple.config.flowable.command;

import com.cat.common.entity.auth.LoginUser;
import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.hook.PassContext;
import com.cat.simple.config.flowable.variable.VariableNames;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.process.service.ProcessFormService;
import jakarta.annotation.Resource;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        ProcessInstance instance = guard.getInstance(param.getProcessInstanceId());

        // 校验并写入表单数据
        boolean hasNodeData = param.getNodeFormData() != null && !param.getNodeFormData().isEmpty();
        boolean hasGlobalData = param.getGlobalFormData() != null && !param.getGlobalFormData().isEmpty();
        if (hasNodeData || hasGlobalData) {
            processFormService.writeFormData(
                    param.getProcessInstanceId(),
                    instance.getProcessDefinitionId(),
                    instance.getProcessDefinitionVersion(),
                    task.getTaskDefinitionKey(),
                    param.getNodeFormData(),
                    param.getGlobalFormData(),
                    false);
        }

        // 注入表单字段为流程变量，供 NATIVE JUEL 表达式使用
        Map<String, Object> allFormData = new HashMap<>();
        if (param.getNodeFormData() != null) allFormData.putAll(param.getNodeFormData());
        if (param.getGlobalFormData() != null) allFormData.putAll(param.getGlobalFormData());

        for (Map.Entry<String, Object> entry : allFormData.entrySet()) {
            variableStore.setRaw(instance.getProcessInstanceId(), entry.getKey(), entry.getValue());
        }
        // 同时注入 formData Map，支持 ${formData['amount']}
        variableStore.set(instance.getProcessInstanceId(), VariableNames.FORM_DATA, allFormData);

        // 注入内置变量（字符串类型）
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser != null) {
            // handler_dept: List<String>
            if (loginUser.getOrgs() != null) {
                List<String> deptIds = loginUser.getOrgs().stream()
                        .map(org -> String.valueOf(org.getId()))
                        .collect(Collectors.toList());
                variableStore.set(instance.getProcessInstanceId(), VariableNames.HANDLER_DEPT, deptIds);
            }
            // handler_role: List<String>
            if (loginUser.getRoles() != null) {
                List<String> roleIds = loginUser.getRoles().stream()
                        .map(role -> String.valueOf(role.getId()))
                        .collect(Collectors.toList());
                variableStore.set(instance.getProcessInstanceId(), VariableNames.HANDLER_ROLE, roleIds);
            }
        }

        // 网关 CUSTOM 条件由 GatewayConditionEvaluator 在 Flowable 评估出线时懒加载计算
        // 这里不再预计算，避免错误地以当前任务节点作为 sourceNodeId 导致漏算

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
                param.getRemark(), param.getNodeFormData(), param.getGlobalFormData());
        lifecycleHook.beforePass(ctx);
    }

    @Override
    protected void afterHook(Void result) {
        lifecycleHook.afterPass(guard.getInstance(param.getProcessInstanceId()));
    }
}