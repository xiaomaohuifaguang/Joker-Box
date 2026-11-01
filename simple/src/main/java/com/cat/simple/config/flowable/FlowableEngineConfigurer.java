package com.cat.simple.config.flowable;

import com.cat.simple.config.flowable.listener.NodeRoundEventListener;
import com.cat.simple.config.flowable.parse.ApprovalUserTaskParseHandler;
import jakarta.annotation.Resource;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.parse.BpmnParseHandler;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import java.util.*;

/**
 * Flowable 引擎配置，将自定义的 {@link ApprovalUserTaskParseHandler} }
 * 注册到引擎的 pre-parse 处理器链中，确保在 Flowable 默认解析器之前完成扩展元素的翻译。
 */
@Configuration
public class FlowableEngineConfigurer {


    @Resource
    private NodeRoundEventListener nodeRoundEventListener;

    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> approvalParseHandlerConfigurer() {
        return configuration -> {
            List<BpmnParseHandler> handlers = new ArrayList<>();
            if (configuration.getPreBpmnParseHandlers() != null) {
                handlers.addAll(configuration.getPreBpmnParseHandlers());
            }
            handlers.add(new ApprovalUserTaskParseHandler());
            configuration.setPreBpmnParseHandlers(handlers);


            Map<String, List<FlowableEventListener>> typed = new HashMap<>();
            typed.put(FlowableEngineEventType.ACTIVITY_STARTED.name(),
                    Collections.singletonList(nodeRoundEventListener));
            typed.put(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED.name(),
                    Collections.singletonList(nodeRoundEventListener));
            configuration.setTypedEventListeners(typed);

        };
    }
}