package com.cat.simple.config.flowable.approval.handler;

import com.cat.simple.config.flowable.approval.ApprovalContext;
import com.cat.simple.config.flowable.approval.ApprovalTypeEnum;
import com.cat.simple.config.flowable.approval.ApprovalTypeHandler;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;



/**
 * 认领处理器（approvalType=4）。
 * 不预先指派 assignee，把候选池全部加入 candidateUser，等待用户主动 claim。
 */
@Slf4j
@Component
public class ClaimHandler implements ApprovalTypeHandler {



    @Override
    public ApprovalTypeEnum supports() {
        return ApprovalTypeEnum.CLAIM;
    }

    @Override
    public void applyOnCreate(DelegateTask task, ApprovalContext ctx) {

    }
}