package com.cat.simple.config.flowable.approval.handler;

import com.cat.simple.config.flowable.approval.ApprovalContext;
import com.cat.simple.config.flowable.approval.ApprovalTypeEnum;
import com.cat.simple.config.flowable.approval.ApprovalTypeHandler;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;



/**
 * 随机1人处理器（approvalType=3）。
 * 从候选池中随机抽取 1 人直接设为 assignee。
 */
@Slf4j
@Component
public class RandomHandler implements ApprovalTypeHandler {


    @Override
    public ApprovalTypeEnum supports() {
        return ApprovalTypeEnum.RANDOM;
    }

    @Override
    public void applyOnCreate(DelegateTask task, ApprovalContext ctx) {

    }
}