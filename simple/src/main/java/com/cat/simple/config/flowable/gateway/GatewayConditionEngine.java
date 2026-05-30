package com.cat.simple.config.flowable.gateway;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.process.ProcessGatewayCondition;
import com.cat.common.entity.process.ProcessGatewayConditionNode;
import com.cat.simple.config.flowable.gateway.operator.ConditionOperator;
import com.cat.simple.config.flowable.gateway.operator.OperatorFactory;
import com.cat.simple.process.mapper.ProcessGatewayConditionMapper;
import com.cat.simple.process.mapper.ProcessGatewayConditionNodeMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Component
public class GatewayConditionEngine {

    @Resource
    private ProcessGatewayConditionMapper conditionMapper;
    @Resource
    private ProcessGatewayConditionNodeMapper nodeMapper;
    @Resource
    private OperatorFactory operatorFactory;
    @Resource
    private GatewayEvaluationContext evaluationContext;

    public boolean evaluate(Integer processDefinitionId, String version,
                            String sequenceFlowId, Integer processInstanceId, String nodeId) {
        ProcessGatewayCondition condition = conditionMapper.selectOne(
                new LambdaQueryWrapper<ProcessGatewayCondition>()
                        .eq(ProcessGatewayCondition::getProcessDefinitionId, processDefinitionId)
                        .eq(ProcessGatewayCondition::getVersion, version)
                        .eq(ProcessGatewayCondition::getSequenceFlowId, sequenceFlowId));

        if (condition == null || Boolean.TRUE.equals(condition.getIsDefault())) {
            return true;
        }
        if (!"CUSTOM".equals(condition.getConditionType())) {
            return true;
        }

        List<ProcessGatewayConditionNode> ruleTree = loadRuleTree(condition.getId());
        if (CollectionUtils.isEmpty(ruleTree)) {
            return true;
        }

        evaluationContext.init(processInstanceId, nodeId);
        return evaluateNode(ruleTree.get(0));
    }

    public Map<String, Boolean> evaluateAll(Integer processDefinitionId, String version,
                                            String sourceNodeId, Integer processInstanceId, String nodeId) {
        List<ProcessGatewayCondition> conditions = conditionMapper.selectList(
                new LambdaQueryWrapper<ProcessGatewayCondition>()
                        .eq(ProcessGatewayCondition::getProcessDefinitionId, processDefinitionId)
                        .eq(ProcessGatewayCondition::getVersion, version)
                        .eq(ProcessGatewayCondition::getSourceNodeId, sourceNodeId)
                        .eq(ProcessGatewayCondition::getConditionType, "CUSTOM"));

        if (CollectionUtils.isEmpty(conditions)) {
            return Collections.emptyMap();
        }

        evaluationContext.init(processInstanceId, nodeId);
        Map<String, Boolean> results = new HashMap<>();

        for (ProcessGatewayCondition condition : conditions) {
            List<ProcessGatewayConditionNode> ruleTree = loadRuleTree(condition.getId());
            boolean result = CollectionUtils.isEmpty(ruleTree) || evaluateNode(ruleTree.get(0));
            results.put(condition.getSequenceFlowId(), result);
        }

        return results;
    }

    public List<ProcessGatewayConditionNode> loadRuleTree(Long conditionId) {
        List<ProcessGatewayConditionNode> nodes = nodeMapper.selectList(
                new LambdaQueryWrapper<ProcessGatewayConditionNode>()
                        .eq(ProcessGatewayConditionNode::getConditionId, conditionId)
                        .orderByAsc(ProcessGatewayConditionNode::getSort));

        if (CollectionUtils.isEmpty(nodes)) {
            return Collections.emptyList();
        }

        Map<Long, List<ProcessGatewayConditionNode>> parentToChildren = new HashMap<>();
        for (ProcessGatewayConditionNode node : nodes) {
            parentToChildren.computeIfAbsent(node.getParentId() == null ? 0L : node.getParentId(),
                    k -> new ArrayList<>()).add(node);
        }

        List<ProcessGatewayConditionNode> roots = parentToChildren.getOrDefault(0L, Collections.emptyList());
        for (ProcessGatewayConditionNode root : roots) {
            attachChildren(root, parentToChildren);
        }
        return roots;
    }

    private void attachChildren(ProcessGatewayConditionNode parent,
                                Map<Long, List<ProcessGatewayConditionNode>> parentToChildren) {
        List<ProcessGatewayConditionNode> children = parentToChildren.get(parent.getId());
        if (!CollectionUtils.isEmpty(children)) {
            parent.setChildren(children);
            for (ProcessGatewayConditionNode child : children) {
                attachChildren(child, parentToChildren);
            }
        }
    }

    private boolean evaluateNode(ProcessGatewayConditionNode node) {
        if (node == null) return true;

        return switch (node.getNodeType()) {
            case "AND" -> {
                if (CollectionUtils.isEmpty(node.getChildren())) yield true;
                yield node.getChildren().stream().allMatch(this::evaluateNode);
            }
            case "OR" -> {
                if (CollectionUtils.isEmpty(node.getChildren())) yield true;
                yield node.getChildren().stream().anyMatch(this::evaluateNode);
            }
            case "CONDITION" -> evaluateCondition(node);
            default -> true;
        };
    }

    private boolean evaluateCondition(ProcessGatewayConditionNode node) {
        Object actualValue = evaluationContext.getValue(node.getCategory(), node.getFieldKey());
        ConditionOperator operator = operatorFactory.get(node.getOperator());
        return operator.compare(actualValue, node.getValue());
    }
}
