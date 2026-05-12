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
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机1人处理器（approvalType=3）。
 * 从候选池中随机抽取 1 人直接设为 assignee。
 */
@Slf4j
@Component
public class RandomHandler implements ApprovalTypeHandler {

    @Resource
    private CandidateResolver candidateResolver;

    @Override
    public ApprovalTypeEnum supports() {
        return ApprovalTypeEnum.RANDOM;
    }

    @Override
    public void applyOnCreate(DelegateTask task, ApprovalContext ctx) {
        List<String> pool = candidateResolver.resolve(ctx);
        if (pool.isEmpty()) {
            log.warn("[随机1人] 候选池为空, taskId={}", task.getId());
            return;
        }
        String picked = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        task.setAssignee(picked);
        log.info("[随机1人] taskId={}, 候选池大小={}, 指派={}", task.getId(), pool.size(), picked);
    }
}