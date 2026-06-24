package com.cat.simple.config.flowable.gateway;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.process.mapper.ProcessInstanceMapper;
import jakarta.annotation.Resource;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Flowable JUEL 表达式中调用的入口 Bean。
 * CUSTOM 模式的 sequenceFlow conditionExpression 形如：
 * <pre>
 * ${gatewayConditionEvaluator.evaluate(execution, 'gatewayId', 'sequenceFlowId')}
 * </pre>
 *
 * <p>采用懒加载策略：Flowable 评估到某条出线时才计算该网关的所有 CUSTOM 条件，
 * 结果缓存到 execution 局部变量中，同网关的其他出线评估直接命中缓存。</p>
 */
@Component("gatewayConditionEvaluator")
public class GatewayConditionEvaluator {

    private static final String CACHE_KEY_PREFIX = "__gw_cache_";

    @Resource
    private GatewayConditionEngine gatewayConditionEngine;

    @Resource
    private ProcessInstanceMapper processInstanceMapper;

    /**
     * 评估某条 sequenceFlow 的条件是否满足。
     *
     * @param execution      Flowable 注入的当前执行上下文
     * @param gatewayId      所属网关节点ID（即 sequenceFlow 的 sourceNodeId）
     * @param sequenceFlowId 要评估的 sequenceFlow ID
     */
    @SuppressWarnings("unchecked")
    public boolean evaluate(DelegateExecution execution, String gatewayId, String sequenceFlowId) {
        if (execution == null || gatewayId == null || sequenceFlowId == null) {
            return false;
        }

        String cacheKey = CACHE_KEY_PREFIX + gatewayId;
        Map<String, Boolean> results = (Map<String, Boolean>) execution.getVariableLocal(cacheKey);

        if (results == null) {
            results = computeGatewayResults(execution, gatewayId);
            execution.setVariableLocal(cacheKey, results);
        }

        return results.getOrDefault(sequenceFlowId, false);
    }

    private Map<String, Boolean> computeGatewayResults(DelegateExecution execution, String gatewayId) {
        // 反查业务流程实例信息（processDefinitionId + version）
        ProcessInstance instance = processInstanceMapper.selectOne(
                new LambdaQueryWrapper<ProcessInstance>()
                        .eq(ProcessInstance::getProcessInstanceId, execution.getProcessInstanceId()));

        if (instance == null) {
            return java.util.Collections.emptyMap();
        }

        return gatewayConditionEngine.evaluateAll(
                instance.getProcessDefinitionId(),
                instance.getProcessDefinitionVersion(),
                gatewayId,
                instance.getId(),
                gatewayId);
    }
}