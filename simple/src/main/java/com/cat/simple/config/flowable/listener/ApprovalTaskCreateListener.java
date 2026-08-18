package com.cat.simple.config.flowable.listener;


import com.cat.simple.config.flowable.approval.ApprovalContext;
import com.cat.simple.config.flowable.approval.ApprovalTypeEnum;
import com.cat.simple.config.flowable.approval.ApprovalTypeHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 审批任务创建监听器，由 {@link com.cat.simple.config.flowable.parse.ApprovalUserTaskParseHandler}
 * 在解析期为每个带 approvalType 的 UserTask 注册。
 * 通过 Spring Bean 表达式 {@code ${approvalTaskCreateListener}} 被 Flowable 调用。
 */
@Slf4j
@Component("approvalTaskCreateListener")
public class ApprovalTaskCreateListener implements TaskListener {

    @Resource
    private List<ApprovalTypeHandler> handlers;

    @Resource
    private RepositoryService repositoryService;

    private Map<ApprovalTypeEnum, ApprovalTypeHandler> handlerMap;

    /**
     * 将所有 ApprovalTypeHandler Bean 按支持类型建立索引，便于 O(1) 分发。
     */
    @PostConstruct
    public void init() {
        handlerMap = new EnumMap<>(ApprovalTypeEnum.class);
        for (ApprovalTypeHandler handler : handlers) {
            handlerMap.put(handler.supports(), handler);
        }
    }


    @Override
    public void notify(DelegateTask delegateTask) {
        UserTask userTask = lookupUserTask(delegateTask);
        if (userTask == null) {
            return;
        }
        ApprovalContext ctx = ApprovalContext.from(userTask);
        if (ctx != null) {
            ApprovalTypeHandler handler = handlerMap.get(ctx.type());
            if (handler == null) {
                log.warn("approvalType={} 无匹配 handler, taskId={}", ctx.type(), delegateTask.getId());
            } else {
                handler.applyOnCreate(delegateTask, ctx);
            }
        }
    }

    /**
     * 根据 task 中的流程定义信息反查 BPMN 模型中的 UserTask 节点。
     *
     * @param delegateTask Flowable 任务委托对象
     * @return 对应的 BPMN UserTask 模型，找不到返回 {@code null}
     */
    private UserTask lookupUserTask(DelegateTask delegateTask) {
        BpmnModel model = repositoryService.getBpmnModel(delegateTask.getProcessDefinitionId());
        if (model == null) {
            return null;
        }
        return model.getFlowElement(delegateTask.getTaskDefinitionKey()) instanceof UserTask ut ? ut : null;
    }

}