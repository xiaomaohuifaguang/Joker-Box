package com.cat.simple.config.flowable.approval.handler;

import com.cat.simple.config.flowable.approval.ApprovalContext;
import com.cat.simple.config.flowable.approval.ApprovalTypeEnum;
import com.cat.simple.config.flowable.approval.ApprovalTypeHandler;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

/**
 * 会签处理器（approvalType=1）。
 * 多实例配置与完成条件已在解析期写入 BPMN 模型，
 * 每个实例的 assignee 由 Flowable 多实例 elementVariable 自动注入，
 * 这里仅做日志留痕。
 */
@Slf4j
@Component
public class CountersignHandler implements ApprovalTypeHandler {

    @Override
    public ApprovalTypeEnum supports() {
        return ApprovalTypeEnum.COUNTERSIGN;
    }

    @Override
    public void applyOnCreate(DelegateTask task, ApprovalContext ctx) {
        log.info("[会签] taskId={}, assignee={}, passRate={}",
                task.getId(), task.getAssignee(), ctx.passRate());
    }
}