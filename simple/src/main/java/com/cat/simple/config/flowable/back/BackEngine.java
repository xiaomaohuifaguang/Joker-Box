package com.cat.simple.config.flowable.back;

import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.enums.BackAssigneePolicyEnum;
import com.cat.simple.config.flowable.util.BpmnModelUtil;
import com.cat.simple.config.flowable.variable.ProcessVariableStore;
import com.cat.simple.config.flowable.variable.VariableNames;
import jakarta.annotation.Resource;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.Execution;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
public class BackEngine {

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private TaskService taskService;

    @Resource
    private BackAssigneeResolver backAssigneeResolver;

    @Resource
    private BpmnModelUtil bpmnModelUtil;

    @Resource
    private ProcessVariableStore variableStore;

    public void backSingleInstance(ProcessInstance instance, Task task, String targetNodeId,
                                    String currentUserId, String remark, String backAssigneePolicy,
                                    String targetNodeName) {
        // 校验目标节点是否已有进行中的任务
        long activeCount = taskService.createTaskQuery()
                .processInstanceId(task.getProcessInstanceId())
                .taskDefinitionKey(targetNodeId)
                .count();
        if (activeCount > 0) {
            throw new IllegalStateException("目标节点已有进行中的任务，无法驳回");
        }

        // REASSIGN 策略不干预 collection，其他策略注入历史处理人
        String backVarName = VariableNames.BACK_ASSIGNEES_PREFIX.getKey() + targetNodeId;
        List<String> prevRoundHandlers = List.of();
        boolean intervene = !BackAssigneePolicyEnum.REASSIGN.getCode().equals(backAssigneePolicy);
        if (intervene) {
            prevRoundHandlers = backAssigneeResolver.findPrevRoundHandlers(
                    instance.getProcessInstanceId(), targetNodeId, task.getCreateTime());
            if (!prevRoundHandlers.isEmpty()) {
                variableStore.setRaw(instance.getProcessInstanceId(), backVarName, prevRoundHandlers);
            }
        }

        try {
            // Flowable 改状态
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(task.getProcessInstanceId())
                    .moveExecutionToActivityId(task.getExecutionId(), targetNodeId)
                    .changeState();

            // 新任务分派
            List<Task> newTasks = taskService.createTaskQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .taskDefinitionKey(targetNodeId)
                    .list();
            assignBackTasks(newTasks, backAssigneePolicy, instance, targetNodeId, prevRoundHandlers);
        } finally {
            if (intervene) {
                variableStore.removeRaw(instance.getProcessInstanceId(), backVarName);
            }
        }
    }

    public void backMultiInstanceAllBack(ProcessInstance instance, Task task, String targetNodeId,
                                          String currentUserId, String remark, String backAssigneePolicy,
                                          String targetNodeName) {
        // 校验目标节点
        long activeCount = taskService.createTaskQuery()
                .processInstanceId(task.getProcessInstanceId())
                .taskDefinitionKey(targetNodeId)
                .count();
        if (activeCount > 0) {
            throw new IllegalStateException("目标节点已有进行中的任务，无法驳回");
        }

        // 找到多实例父执行
        Execution childExecution = runtimeService.createExecutionQuery()
                .executionId(task.getExecutionId())
                .singleResult();
        if (childExecution == null || childExecution.getParentId() == null) {
            throw new IllegalStateException("无法定位多实例父执行");
        }
        Execution miBody = runtimeService.createExecutionQuery()
                .executionId(childExecution.getParentId())
                .singleResult();
        if (miBody == null) {
            throw new IllegalStateException("无法定位多实例父执行");
        }

        // REASSIGN 策略不干预 collection，其他策略注入历史处理人
        String backVarName = VariableNames.BACK_ASSIGNEES_PREFIX.getKey() + targetNodeId;
        List<String> prevRoundHandlers = List.of();
        boolean intervene = !BackAssigneePolicyEnum.REASSIGN.getCode().equals(backAssigneePolicy);
        if (intervene) {
            prevRoundHandlers = backAssigneeResolver.findPrevRoundHandlers(
                    instance.getProcessInstanceId(), targetNodeId, task.getCreateTime());
            if (!prevRoundHandlers.isEmpty()) {
                variableStore.setRaw(instance.getProcessInstanceId(), backVarName, prevRoundHandlers);
            }
        }

        try {
            // 用父执行改状态
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(task.getProcessInstanceId())
                    .moveExecutionToActivityId(miBody.getId(), targetNodeId)
                    .changeState();

            // 新任务分派
            List<Task> newTasks = taskService.createTaskQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .taskDefinitionKey(targetNodeId)
                    .list();
            assignBackTasks(newTasks, backAssigneePolicy, instance, targetNodeId, prevRoundHandlers);
        } finally {
            if (intervene) {
                variableStore.removeRaw(instance.getProcessInstanceId(), backVarName);
            }
        }
    }

    public void assignBackTasks(List<Task> newTasks, String backAssigneePolicy,
                                 ProcessInstance instance, String targetNodeId,
                                 List<String> prevRoundHandlers) {
        if (CollectionUtils.isEmpty(newTasks)) return;

        boolean targetIsMultiInstance = bpmnModelUtil.isMultiInstance(
                newTasks.get(0).getProcessDefinitionId(), targetNodeId);

        if (targetIsMultiInstance) {
            // 多实例目标节点：由 CandidateResolver + 多实例处理器自动处理 assignee
            // 干预时已通过 __back_assignees_{targetNodeId} 变量注入历史处理人
            // 不干预时 CandidateResolver 按候选配置解析
            return;
        }

        // 单实例目标节点：按策略手动设置 assignee
        for (Task newTask : newTasks) {
            String assignee = backAssigneeResolver.resolveAssignee(newTask, backAssigneePolicy, instance);
            if (assignee != null) {
                taskService.setAssignee(newTask.getId(), assignee);
            }
        }
    }
}