package com.cat.simple.config.flowable.approval.handler;

import com.cat.simple.config.flowable.approval.ApprovalContext;
import com.cat.simple.config.flowable.approval.ApprovalTypeEnum;
import com.cat.simple.config.flowable.approval.ApprovalTypeHandler;
import com.cat.simple.config.flowable.candidate.CandidateResolver;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 认领处理器（approvalType=4）。
 * 不预先指派 assignee，把候选池全部加入 candidateUser，等待用户主动 claim。
 */
@Slf4j
@Component
public class ClaimHandler implements ApprovalTypeHandler {

    @Resource
    private CandidateResolver candidateResolver;

    @Override
    public ApprovalTypeEnum supports() {
        return ApprovalTypeEnum.CLAIM;
    }

    @Override
    public void applyOnCreate(DelegateTask task, ApprovalContext ctx) {
        List<String> pool = candidateResolver.resolve(ctx);
        if (pool.isEmpty()) {
            log.warn("[认领] 候选池为空, taskId={}", task.getId());
            return;
        }
        pool.forEach(task::addCandidateUser);
        log.info("[认领] taskId={}, 候选人数={}", task.getId(), pool.size());
    }
}