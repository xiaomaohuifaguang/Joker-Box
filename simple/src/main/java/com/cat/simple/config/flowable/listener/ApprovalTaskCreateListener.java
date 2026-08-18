package com.cat.simple.config.flowable.listener;


import com.cat.simple.config.flowable.approval.ApprovalContext;
import com.cat.simple.config.flowable.approval.ApprovalTypeEnum;
import com.cat.simple.config.flowable.approval.ApprovalTypeHandler;
import com.cat.simple.config.flowable.candidate.CandidateResolver;
import com.cat.simple.process.service.ProcessInstanceService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

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

    @Resource
    private CandidateResolver candidateResolver;

    @Resource
    private ProcessInstanceService processInstanceService;

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
            maybeAutoApprove(delegateTask, ctx);
        }
    }

    /**
     * 节点配置 autoApproveIfSelf=1 且任务办理人与申请人一致时，登记事务提交后的自动通过。
     * 不能在 create 事件里直接 complete（任务尚未落库、且无登录上下文），
     * 故延迟到当前事务提交后由 ProcessInstanceService.autoPass 在新事务中执行。
     */
    private void maybeAutoApprove(DelegateTask delegateTask, ApprovalContext ctx) {
        if (!"1".equals(ctx.autoApproveIfSelf())) {
            return;
        }
        String assignee = delegateTask.getAssignee();
        if (!StringUtils.hasText(assignee)) {
            return;
        }
        String applicant = candidateResolver.findApplicant(delegateTask.getProcessInstanceId());
        if (!assignee.equals(applicant)) {
            return;
        }
        String taskId = delegateTask.getId();
        log.info("[自动通过] 命中 autoApproveIfSelf, taskId={}, assignee={}", taskId, assignee);
        Runnable autoPass = () -> {
            try {
                processInstanceService.autoPass(taskId);
            } catch (Exception e) {
                log.error("[自动通过] 执行失败, taskId={}，任务保持待办", taskId, e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    autoPass.run();
                }
            });
        } else {
            autoPass.run();
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