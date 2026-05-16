package com.cat.simple.config.flowable.back;

import com.cat.common.entity.process.BackConfig;
import com.cat.common.entity.process.BackTargetNode;
import com.cat.simple.config.flowable.enums.BackAssigneePolicyEnum;
import com.cat.simple.config.flowable.variable.ProcessVariableStore;
import com.cat.simple.config.flowable.variable.VariableNames;
import jakarta.annotation.Resource;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class BackConfigReader {

    @Resource
    private TaskService taskService;

    @Resource
    private HistoryService historyService;

    @Resource
    private ProcessVariableStore variableStore;

    public List<BackTargetNode> getAvailableBackTargets(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        List<HistoricActivityInstance> userTasks = historyService
                .createHistoricActivityInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .activityType("userTask")
                .orderByHistoricActivityInstanceEndTime().desc()
                .list();

        Map<String, BackTargetNode> targets = new LinkedHashMap<>();
        for (HistoricActivityInstance h : userTasks) {
            String nodeId = h.getActivityId();
            if (nodeId.equals(task.getTaskDefinitionKey())) continue;

            targets.putIfAbsent(nodeId, new BackTargetNode()
                    .setNodeId(nodeId)
                    .setNodeName(h.getActivityName()));
        }
        return new ArrayList<>(targets.values());
    }

    public BackConfig getBackConfig(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        String actionButtons = variableStore.getLocal(task, VariableNames.ACTION_BUTTONS, String.class);
        String backType = variableStore.getLocal(task, VariableNames.BACK_TYPE, String.class);
        String backNodeId = variableStore.getLocal(task, VariableNames.BACK_NODE_ID, String.class);
        String backAssigneePolicy = variableStore.getLocal(task, VariableNames.BACK_ASSIGNEE_POLICY, String.class);

        BackConfig config = new BackConfig();
        config.setAllowBack(backType != null && !backType.isBlank());
        config.setBackType(backType);
        config.setBackNodeId(backNodeId);
        config.setBackAssigneePolicy(backAssigneePolicy != null ? backAssigneePolicy : BackAssigneePolicyEnum.AUTO.getCode());
        config.setActionButtons(actionButtons != null && !actionButtons.isBlank()
                ? Arrays.asList(actionButtons.split(",")) : List.of());
        return config;
    }
}