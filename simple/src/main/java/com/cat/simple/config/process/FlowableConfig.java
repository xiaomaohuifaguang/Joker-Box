package com.cat.simple.config.process;

import com.cat.simple.config.process.core.BpmActivityBehaviorFactory;

import org.flowable.common.engine.api.delegate.FlowableFunctionDelegate;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import java.util.stream.Collectors;

@Configuration
public class FlowableConfig{



    /**
     * BPM 模块的 ProcessEngineConfigurationConfigurer 实现类：
     *
     * 1. 设置各种监听器
     * 2. 设置自定义的 ActivityBehaviorFactory 实现
     */
    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> bpmProcessEngineConfigurationConfigurer(
            ObjectProvider<FlowableEventListener> listeners,
            ObjectProvider<FlowableFunctionDelegate> customFlowableFunctionDelegates,
            BpmActivityBehaviorFactory bpmActivityBehaviorFactory
    ){

        return configuration -> {
            // 注册监听器，例如说 BpmActivityEventListener
            configuration.setEventListeners(listeners.stream().collect(Collectors.toList()));
            // 设置自定义的函数
            configuration.setCustomFlowableFunctionDelegates(customFlowableFunctionDelegates.stream().collect(Collectors.toList()));

            // 设置 ActivityBehaviorFactory 实现类，用于流程任务的审核人的自定义
            configuration.setActivityBehaviorFactory(bpmActivityBehaviorFactory);

        };


    }


    // =========== 审批人相关的 Bean ==========
    @Bean
    public BpmActivityBehaviorFactory bpmActivityBehaviorFactory() {
        return new BpmActivityBehaviorFactory();
    }


//
//
//    @Bean
//    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // adminUserApi 可以注入成功
//    public BpmTaskCandidateInvoker bpmTaskCandidateInvoker(List<BpmTaskCandidateStrategy> strategyList) {
//        return new BpmTaskCandidateInvoker(strategyList);
//    }


}
