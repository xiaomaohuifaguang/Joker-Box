package com.cat.simple.config.flowable.approval.handler;

import com.cat.simple.config.flowable.approval.ApprovalContext;
import com.cat.simple.config.flowable.approval.ApprovalTypeEnum;
import com.cat.simple.config.flowable.approval.ApprovalTypeHandler;
import com.cat.simple.config.flowable.candidate.CandidateResolver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 申请人自审（approvalType=0）。
 * 从候选池中随机抽取 1 人直接设为 assignee。
 */
@Slf4j
@Component
public class ApplicantSelfHandler implements ApprovalTypeHandler {

    @Resource
    private CandidateResolver candidateResolver;

    @Override
    public ApprovalTypeEnum supports() {
        return ApprovalTypeEnum.APPLICANT_SELF;
    }

    @Override
    public void applyOnCreate(DelegateTask task, ApprovalContext ctx) {

        List<String> pool = candidateResolver.resolve(ctx, task.getProcessInstanceId());
            if (pool.isEmpty()) {
            log.warn("[申请人自审] 候选池为空, taskId={}", task.getId());
            return;
        }
        task.setAssignee(pool.get(0));
        log.info("[申请人自审] taskId={}, 候选池大小={}, 指派={}", task.getId(), pool.size(), pool.get(0));

    }
}