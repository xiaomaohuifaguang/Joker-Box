package com.cat.simple.config.flowable.back;

import com.cat.simple.config.flowable.enums.BackTypeEnum;
import jakarta.annotation.Resource;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

@Component
public class BackTargetResolver {

    @Resource
    private HistoryService historyService;

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private RepositoryService repositoryService;

    public String resolveTargetNodeId(Task task, String backType, String backNodeId, String paramTargetNodeId) {
        BackTypeEnum type = BackTypeEnum.of(backType);
        if (type == null) {
            throw new IllegalStateException("不支持的驳回方式: " + backType);
        }
        return switch (type) {
            case PREV -> resolvePrevNodeId(task);
            case SPECIFIC -> {
                if (backNodeId == null || backNodeId.isBlank()) {
                    throw new IllegalStateException("该节点未配置固定驳回目标");
                }
                yield backNodeId;
            }
            case CHOOSE -> {
                if (paramTargetNodeId == null || paramTargetNodeId.isBlank()) {
                    throw new IllegalArgumentException("请选择驳回目标节点");
                }
                validateTargetNode(task.getProcessInstanceId(), paramTargetNodeId);
                yield paramTargetNodeId;
            }
        };
    }

    public String resolvePrevNodeId(Task task) {
        HistoricActivityInstance last = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .activityType("userTask")
                .finishedBefore(task.getCreateTime())
                .orderByHistoricActivityInstanceEndTime().desc()
                .list().stream()
                .filter(h -> !h.getActivityId().equals(task.getTaskDefinitionKey()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("没有可驳回的上级节点"));
        return last.getActivityId();
    }

    public void validateTargetNode(String processInstanceId, String targetNodeId) {
        long count = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .activityId(targetNodeId)
                .activityType("userTask")
                .count();
        if (count == 0) {
            throw new IllegalArgumentException("无效的回退目标节点: " + targetNodeId);
        }
    }

    public String resolveTargetNodeName(String processInstanceId, String targetNodeId) {
        BpmnModel model = repositoryService.getBpmnModel(
                runtimeService.createProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult()
                        .getProcessDefinitionId());
        if (model != null && model.getFlowElement(targetNodeId) instanceof UserTask ut) {
            return ut.getName();
        }
        return targetNodeId;
    }

}