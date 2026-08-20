package com.cat.simple.config.flowable.engine;

import com.cat.common.entity.process.BackConfig;
import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.simple.config.flowable.approval.ApprovalContext;
import com.cat.simple.config.flowable.candidate.CandidateResolver;
import com.cat.simple.config.flowable.enums.BackAssigneePolicyEnum;
import com.cat.simple.config.flowable.enums.BackTypeEnum;
import com.cat.simple.config.flowable.util.FlowableUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Objects;

import static com.cat.common.entity.process.constants.VariablesConstants.CACHE_ASSIGNEES;

@Slf4j
@Component
public class BackEngine {

    @Resource private FlowableUtils flowableUtils;
    @Resource private HistoryService historyService;
    @Resource private RepositoryService repositoryService;
    @Resource private RuntimeService runtimeService;
    @Resource private TaskService taskService;
    @Resource private CandidateResolver candidateResolver;

    public void back(ProcessHandleParam param, Task task){

        BackConfig cfg = flowableUtils.getBackConfig(param.getTaskId());

        BackTypeEnum type = BackTypeEnum.of(cfg.getBackType());
        if (type == null) {
            throw new IllegalStateException("不支持的驳回方式: " + cfg.getBackType());
        }

        if (!cfg.isAllowBack()) {
            throw new IllegalStateException("该节点未配置驳回方式, taskId: " + param.getTaskId());
        }

        UserTask backUserTask = null;
        switch (type) {
            case PREV -> {
                backUserTask = getBackUserTaskPrev(task);
            }
            case SPECIFIC -> {
                if (cfg.getBackNodeId() == null || cfg.getBackNodeId().isBlank()) {
                    throw new IllegalStateException("该节点未配置固定驳回目标");
                }
                backUserTask = getBackUserTask(task.getProcessInstanceId(), cfg.getBackNodeId());
            }
            case CHOOSE -> {
                if (param.getTargetNodeId() == null || param.getTargetNodeId().isBlank()) {
                    throw new IllegalArgumentException("请选择驳回目标节点");
                }
                validateTargetNode(task.getProcessInstanceId(), param.getTargetNodeId());
                backUserTask = getBackUserTask(task.getProcessInstanceId(), param.getTargetNodeId());
            }
        };
        if(Objects.isNull(backUserTask)){
            throw new IllegalArgumentException("上一用户任务未查到/不存在");
        }
        param.setTargetNodeId(backUserTask.getId());
        param.setTargetNodeName(backUserTask.getName());


        // 判断当前任务是否多实例
        boolean isMultiInstance = flowableUtils.isMultiInstance(task);


        // 校验目标节点是否已有进行中的任务
        long activeCount = taskService.createTaskQuery()
                .processInstanceId(task.getProcessInstanceId())
                .taskDefinitionKey(param.getTargetNodeId())
                .count();
        if (activeCount > 0) {
            throw new IllegalStateException("目标节点已有进行中的任务，无法驳回");
        }


        // 1. 确定要移动的 Execution ID（单实例 vs 多实例）
        String targetExecutionId;
        if (isMultiInstance) {
            // 多实例场景：需要找到多实例父执行（Multi-Instance Body）

            // 更优的写法：直接查父执行
            Execution childExecution = runtimeService.createExecutionQuery()
                    .executionId(task.getExecutionId())
                    .singleResult();

            if (childExecution == null || childExecution.getParentId() == null) {
                throw new IllegalStateException("无法定位多实例父执行");
            }
            targetExecutionId = childExecution.getParentId();
        } else {
            // 单实例场景：直接使用当前任务的 Execution ID
            targetExecutionId = task.getExecutionId();
        }

        boolean isTargetNodeMultiInstance = flowableUtils.isMultiInstance(task.getProcessDefinitionId(), param.getTargetNodeId());
        if(isTargetNodeMultiInstance && BackAssigneePolicyEnum.of(cfg.getBackAssigneePolicy()).equals(BackAssigneePolicyEnum.REASSIGN)){
            runtimeService.removeVariable(task.getProcessInstanceId(), CACHE_ASSIGNEES + param.getTargetNodeId());
        }

        // 2. 统一的 Flowable 状态变更（消除 if-else 重复代码）
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(task.getProcessInstanceId())
                .moveExecutionToActivityId(targetExecutionId, param.getTargetNodeId())
                .changeState();




        if(isTargetNodeMultiInstance){
            // 新任务分派
            List<Task> newTasks = taskService.createTaskQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .taskDefinitionKey(param.getTargetNodeId())
                    .list();
            for (Task newTask : newTasks) {
                log.info(newTask.getAssignee());
            }
        }else {
            // 新任务分派
            Task newTask = taskService.createTaskQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .taskDefinitionKey(param.getTargetNodeId())
                    .singleResult();
            String assignee = resolveAssignee(newTask, cfg.getBackAssigneePolicy());
            taskService.setAssignee(newTask.getId(), assignee);
        }
    }





    private UserTask getBackUserTaskPrev(Task task){

        HistoricActivityInstance last = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .activityType("userTask")
                .finishedBefore(task.getCreateTime())
                .orderByHistoricActivityInstanceEndTime().desc()
                .list().stream()
                .filter(h -> !h.getActivityId().equals(task.getTaskDefinitionKey()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("没有可驳回的上级节点"));

        return getBackUserTask(task.getProcessInstanceId(), last.getActivityId());
    }

    private void validateTargetNode(String processInstanceId, String targetNodeId) {
        long count = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .activityId(targetNodeId)
                .activityType("userTask")
                .count();
        if (count == 0) {
            throw new IllegalArgumentException("无效的回退目标节点: " + targetNodeId);
        }
    }


    private UserTask getBackUserTask(String processInstanceId, String activityId){
        BpmnModel model = repositoryService.getBpmnModel(
                runtimeService.createProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult()
                        .getProcessDefinitionId());

        FlowElement flowElement = model.getFlowElement(activityId);
        if (flowElement instanceof UserTask ut) {
            return ut;
        }

        return null;
    }


    /** 按策略解析回退后任务的办理人。 */
    public String resolveAssignee(Task newTask, String policy) {
        BackAssigneePolicyEnum p = BackAssigneePolicyEnum.of(policy);
        if (p == null) {
            p = BackAssigneePolicyEnum.AUTO;
        }

        return switch (p) {
            case LAST_HANDLER -> findLastHandler(newTask.getProcessInstanceId(), newTask.getTaskDefinitionKey());
            case REASSIGN -> resolveByCandidateConfig(newTask);
            case AUTO -> {
                String last = findLastHandler(newTask.getProcessInstanceId(), newTask.getTaskDefinitionKey());
                yield last != null ? last : resolveByCandidateConfig(newTask);
            }
        };
    }

    /** 查询目标节点最近一次的历史办理人。 */
    public String findLastHandler(String flowableProcessInstanceId, String taskDefinitionKey) {
        return historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(flowableProcessInstanceId)
                .taskDefinitionKey(taskDefinitionKey)
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .list().stream()
                .findFirst()
                .map(HistoricTaskInstance::getAssignee)
                .orElse(null);
    }


    /** 按节点候选配置解析办理人，取第一个候选人。 */
    public String resolveByCandidateConfig(Task task) {
        BpmnModel model = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        if (model == null) return null;
        if (!(model.getFlowElement(task.getTaskDefinitionKey()) instanceof UserTask ut)) return null;

        ApprovalContext ctx = ApprovalContext.from(ut);
        if (ctx == null) return null;

        List<String> assignees = candidateResolver.resolve(ctx, task.getProcessInstanceId());
        if (!ObjectUtils.isEmpty(assignees)) {
            return assignees.get(0);
        }
        return null;
    }

}
