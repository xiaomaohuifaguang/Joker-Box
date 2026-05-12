package com.cat.simple.config.flowable.approval.handler;

import com.cat.simple.config.flowable.approval.ApprovalContext;
import com.cat.simple.config.flowable.approval.ApprovalTypeEnum;
import com.cat.simple.config.flowable.approval.ApprovalTypeHandler;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

/**
 * 或签处理器（approvalType=2）。
 * 多实例配置与完成条件 {@code nrOfCompletedInstances >= 1} 已在解析期写入 BPMN 模型，
 * 任一实例完成即触发整个节点结束，这里仅做日志留痕。
 */
@Slf4j
@Component
public class OrSignHandler implements ApprovalTypeHandler {

    @Override
    public ApprovalTypeEnum supports() {
        return ApprovalTypeEnum.OR_SIGN;
    }

    @Override
    public void applyOnCreate(DelegateTask task, ApprovalContext ctx) {
        log.info("[或签] taskId={}, assignee={}", task.getId(), task.getAssignee());
    }
}