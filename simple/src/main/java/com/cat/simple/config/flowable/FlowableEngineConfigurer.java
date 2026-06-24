package com.cat.simple.config.flowable;

import com.cat.simple.config.flowable.parse.ApprovalUserTaskParseHandler;
import com.cat.simple.config.flowable.parse.SequenceFlowParseHandler;
import jakarta.annotation.Resource;
import org.flowable.engine.parse.BpmnParseHandler;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Flowable 引擎配置，将自定义的 {@link ApprovalUserTaskParseHandler} 和 {@link SequenceFlowParseHandler}
 * 注册到引擎的 pre-parse 处理器链中，确保在 Flowable 默认解析器之前完成扩展元素的翻译。
 */
@Configuration
public class FlowableEngineConfigurer {

    @Resource
    private SequenceFlowParseHandler sequenceFlowParseHandler;

    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> approvalParseHandlerConfigurer() {
        return configuration -> {
            List<BpmnParseHandler> handlers = new ArrayList<>();
            if (configuration.getPreBpmnParseHandlers() != null) {
                handlers.addAll(configuration.getPreBpmnParseHandlers());
            }
            handlers.add(new ApprovalUserTaskParseHandler());
            handlers.add(sequenceFlowParseHandler);
            configuration.setPreBpmnParseHandlers(handlers);
        };
    }
}