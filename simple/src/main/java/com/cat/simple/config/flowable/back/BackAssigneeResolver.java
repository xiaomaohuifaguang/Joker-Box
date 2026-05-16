package com.cat.simple.config.flowable.back;

import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.approval.ApprovalContext;
import com.cat.simple.config.flowable.candidate.CandidateResolver;
import com.cat.simple.config.flowable.enums.BackAssigneePolicyEnum;
import jakarta.annotation.Resource;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Component
public class BackAssigneeResolver {

    @Resource
    private HistoryService historyService;

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private CandidateResolver candidateResolver;

    public String resolveAssignee(Task newTask, String policy, ProcessInstance instance) {
        BackAssigneePolicyEnum p = BackAssigneePolicyEnum.of(policy);
        if (p == null) {
            p = BackAssigneePolicyEnum.AUTO;
        }

        return switch (p) {
            case LAST_HANDLER -> findLastHandler(instance.getProcessInstanceId(), newTask.getTaskDefinitionKey());
            case REASSIGN -> resolveByCandidateConfig(newTask);
            case AUTO -> {
                String last = findLastHandler(instance.getProcessInstanceId(), newTask.getTaskDefinitionKey());
                yield last != null ? last : resolveByCandidateConfig(newTask);
            }
        };
    }

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

    public List<String> findHistoricHandlers(String flowableProcessInstanceId, String taskDefinitionKey) {
        return historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(flowableProcessInstanceId)
                .taskDefinitionKey(taskDefinitionKey)
                .finished()
                .list().stream()
                .map(HistoricTaskInstance::getAssignee)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 查询目标节点"上一轮"的处理人列表。
     * 以当前任务的创建时间为分界，取目标节点在此之前完成的所有历史任务的去重处理人。
     */
    public List<String> findPrevRoundHandlers(String flowableProcessInstanceId, String taskDefinitionKey, Date beforeTime) {
        return historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(flowableProcessInstanceId)
                .taskDefinitionKey(taskDefinitionKey)
                .finished()
                .list().stream()
                .filter(t -> t.getEndTime() != null && t.getEndTime().before(beforeTime))
                .map(HistoricTaskInstance::getAssignee)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    public String resolveByCandidateConfig(Task task) {
        BpmnModel model = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        if (model == null) return null;
        if (!(model.getFlowElement(task.getTaskDefinitionKey()) instanceof UserTask ut)) return null;

        ApprovalContext ctx = ApprovalContext.from(ut);
        if (ctx == null) return null;

        List<String> assignees = candidateResolver.resolve(ctx);
        if (!ObjectUtils.isEmpty(assignees)) {
            return assignees.get(0);
        }
        return null;
    }
}