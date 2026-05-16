package com.cat.simple.config.flowable.util;

import jakarta.annotation.Resource;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

@Component
public class BpmnModelUtil {

    @Resource
    private RepositoryService repositoryService;

    public boolean isMultiInstance(Task task) {
        return isMultiInstance(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
    }

    public boolean isMultiInstance(String processDefinitionId, String taskDefinitionKey) {
        BpmnModel model = repositoryService.getBpmnModel(processDefinitionId);
        if (model == null) return false;
        if (!(model.getFlowElement(taskDefinitionKey) instanceof UserTask ut)) return false;
        return ut.getLoopCharacteristics() != null;
    }
}