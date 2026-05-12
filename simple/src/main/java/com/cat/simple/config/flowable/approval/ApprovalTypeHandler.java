package com.cat.simple.config.flowable.approval;

import org.flowable.task.service.delegate.DelegateTask;

/**
 * 审批类型策略接口，每种审批场景（会签/或签/随机/认领）实现该接口，
 * 由 {@link com.cat.simple.config.flowable.listener.ApprovalTaskCreateListener} 统一派发。
 */
public interface ApprovalTypeHandler {

    /**
     * 返回当前 handler 支持的审批类型。
     */
    ApprovalTypeEnum supports();

    /**
     * 任务创建时的处理逻辑。
     *
     * @param task 当前创建的 DelegateTask
     * @param ctx  从 BPMN 扩展元素解析出的审批上下文
     */
    void applyOnCreate(DelegateTask task, ApprovalContext ctx);
}