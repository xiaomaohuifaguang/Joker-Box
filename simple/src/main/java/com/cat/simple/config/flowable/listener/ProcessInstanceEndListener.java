package com.cat.simple.config.flowable.listener;

import com.cat.simple.config.flowable.enums.ProcessStatusEnum;
import com.cat.simple.service.ProcessInstanceService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.RuntimeService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import static com.cat.simple.config.flowable.enums.ProcessStatusEnum.*;

/**
 * 流程实例结束监听器，监听流程完成、终止事件，将状态同步到业务表。
 */
@Slf4j
@Component
public class ProcessInstanceEndListener implements FlowableEventListener {

    @Resource
    private RuntimeService runtimeService;

    @Resource
    @Lazy
    private ProcessInstanceService processInstanceService;

    /**
     * 注册监听器到 Flowable 运行时服务。
     */
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
                 PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT -> COMPLETED;
            case PROCESS_CANCELLED -> TERMINATED;
            default -> UNKNOWN;
        };
        if (processStatusEnum.equals(UNKNOWN)) {
            return;
        }
        log.info("流程实例 {} 结束, 类型={}, 状态={}", processInstanceId, type, processStatusEnum);
        processInstanceService.updateStatus(processInstanceId, processStatusEnum);
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }

    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        return false;
    }

    @Override
    public String getOnTransaction() {
        return null;
    }
}