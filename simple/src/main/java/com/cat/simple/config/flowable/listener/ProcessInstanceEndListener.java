package com.cat.simple.config.flowable.listener;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.enums.ProcessStatusEnum;
import com.cat.simple.config.flowable.guard.ProcessGuard;
import com.cat.simple.process.mapper.ProcessInstanceMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.RuntimeService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class ProcessInstanceEndListener implements FlowableEventListener {

    @Resource private RuntimeService runtimeService;
    @Resource private ProcessInstanceMapper processInstanceMapper;
    @Resource private ProcessGuard guard;

    @PostConstruct
    public void register() {
        runtimeService.addEventListener(this,
                FlowableEngineEventType.PROCESS_COMPLETED,
                FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT,
                FlowableEngineEventType.PROCESS_CANCELLED);
    }

    @Override
    public void onEvent(FlowableEvent event) {
        if (!(event instanceof FlowableEngineEvent engineEvent)) {
            return;
        }
        String processInstanceId = engineEvent.getProcessInstanceId();
        FlowableEngineEventType type = (FlowableEngineEventType) engineEvent.getType();

        ProcessStatusEnum processStatusEnum = switch (type) {
            case PROCESS_COMPLETED,
                 PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT -> ProcessStatusEnum.COMPLETED;
            case PROCESS_CANCELLED -> ProcessStatusEnum.TERMINATED;
            default -> ProcessStatusEnum.UNKNOWN;
        };
        if (processStatusEnum.equals(ProcessStatusEnum.UNKNOWN)) {
            return;
        }
        ProcessInstance instance = guard.selectByFlowableId(processInstanceId);
        if (instance != null) {
            processInstanceMapper.update(new LambdaUpdateWrapper<ProcessInstance>()
                    .eq(ProcessInstance::getId, instance.getId())
                    .set(ProcessInstance::getProcessStatus, processStatusEnum.getStatus())
                    .set(ProcessInstance::getUpdateTime, LocalDateTime.now()));
            log.info("流程实例 {} 结束, 类型={}, 状态={}", processInstanceId, type, processStatusEnum);
        }
    }

    @Override public boolean isFailOnException() { return false; }
    @Override public boolean isFireOnTransactionLifecycleEvent() { return false; }
    @Override public String getOnTransaction() { return null; }
}
