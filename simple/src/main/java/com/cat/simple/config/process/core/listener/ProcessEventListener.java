package com.cat.simple.config.process.core.listener;

import com.cat.common.entity.process.enums.ProcessStatusEnum;
import com.cat.simple.service.ProcessInstanceService;
import jakarta.annotation.Resource;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ProcessEventListener implements FlowableEventListener {

    @Resource
    @Lazy
    private ProcessInstanceService processInstanceService;
    
    @Override
    public void onEvent(FlowableEvent event) {
        if (event instanceof FlowableEngineEvent engineEvent) {
            if (event.getType() == FlowableEngineEventType.PROCESS_COMPLETED) {
                // 流程完成事件
//                System.out.println("流程完成: " + engineEvent.getProcessInstanceId());
                processInstanceService.updateStatus(engineEvent.getProcessInstanceId(), "COMPLETED", LocalDateTime.now());
            }
        }
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
        return "";
    }
}