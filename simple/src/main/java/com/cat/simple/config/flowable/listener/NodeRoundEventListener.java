package com.cat.simple.config.flowable.listener;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.flowable.engine.delegate.event.FlowableMultiInstanceActivityEvent;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NodeRoundEventListener extends AbstractFlowableEngineEventListener {

    @Override
    protected void activityStarted(FlowableActivityEvent event) {
        countRound(event.getActivityType(), event.getActivityId(),
                event.getExecutionId(), event.getProcessInstanceId());
    }

    @Override
    protected void multiInstanceActivityStarted(FlowableMultiInstanceActivityEvent event) {
        countRound(event.getActivityType(), event.getActivityId(),
                event.getExecutionId(), event.getProcessInstanceId());
    }

    private void countRound(String activityType, String activityId,
                            String executionId, String processInstanceId) {
        if (!"userTask".equals(activityType)) {
            return;
        }
        ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager()
                .findById(executionId);
        if (execution == null) {
            return;
        }
        // ===== 修复点：两条守卫 =====
        // 守卫1：MI 子执行（parent 是 MI root）不计数
        if (execution.getParent() != null && execution.getParent().isMultiInstanceRoot()) {
            return;
        }
        // ==========================
        String varName = "ROUND_" + activityId;
        Integer cur = (Integer) execution.getVariable(varName);
        int next = (cur == null) ? 1 : cur + 1;
        execution.setVariable(varName, next);
        execution.setVariable("ROUND_" + executionId + "_" + activityId, next);
        log.info("节点[{}]进入第 {} 轮, procInstId={}", activityId, next, processInstanceId);
    }

}

