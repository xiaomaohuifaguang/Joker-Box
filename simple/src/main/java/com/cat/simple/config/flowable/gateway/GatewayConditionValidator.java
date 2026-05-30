package com.cat.simple.config.flowable.gateway;

import com.cat.common.entity.process.ProcessGatewayCondition;
import com.cat.common.entity.process.ProcessGatewayConditionNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class GatewayConditionValidator {

    private static final String CONDITION_TYPE_NATIVE = "NATIVE";
    private static final String CONDITION_TYPE_CUSTOM = "CUSTOM";

    private static final String NODE_TYPE_AND = "AND";
    private static final String NODE_TYPE_OR = "OR";
    private static final String NODE_TYPE_CONDITION = "CONDITION";

    private static final String OPERATOR_IN = "IN";
    private static final String OPERATOR_NOT_IN = "NOT_IN";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Validate a list of gateway condition configurations.
     *
     * @param conditions the list to validate
     * @throws IllegalArgumentException if any validation rule fails
     */
    public void validate(List<ProcessGatewayCondition> conditions) {
        if (CollectionUtils.isEmpty(conditions)) {
            return;
        }

        Set<String> sequenceFlowIds = new HashSet<>();
        Set<String> defaultGatewayKeys = new HashSet<>();

        for (ProcessGatewayCondition condition : conditions) {
            validateBasicFields(condition);
            validateSequenceFlowIdUniqueness(condition, sequenceFlowIds);
            validateDefaultUniqueness(condition, defaultGatewayKeys);
            validateConditionTypeAndContent(condition);
        }
    }

    private void validateBasicFields(ProcessGatewayCondition condition) {
        if (!StringUtils.hasText(condition.getSequenceFlowId())) {
            throw new IllegalArgumentException("sequenceFlowId不能为空");
        }
        if (!StringUtils.hasText(condition.getSourceNodeId())) {
            throw new IllegalArgumentException("sourceNodeId不能为空");
        }
        if (!StringUtils.hasText(condition.getTargetNodeId())) {
            throw new IllegalArgumentException("targetNodeId不能为空");
        }
    }

    private void validateSequenceFlowIdUniqueness(ProcessGatewayCondition condition, Set<String> sequenceFlowIds) {
        if (!sequenceFlowIds.add(condition.getSequenceFlowId())) {
            throw new IllegalArgumentException("sequenceFlowId重复: " + condition.getSequenceFlowId());
        }
    }

    private void validateDefaultUniqueness(ProcessGatewayCondition condition, Set<String> defaultGatewayKeys) {
        if (Boolean.TRUE.equals(condition.getIsDefault())) {
            String key = condition.getSourceNodeId();
            if (!defaultGatewayKeys.add(key)) {
                throw new IllegalArgumentException("网关[" + condition.getSourceNodeId() + "]只能有一个默认走向");
            }
        }
    }

    private void validateConditionTypeAndContent(ProcessGatewayCondition condition) {
        if (!Boolean.TRUE.equals(condition.getIsDefault())) {
            if (!StringUtils.hasText(condition.getConditionType())) {
                throw new IllegalArgumentException("非默认走向必须设置conditionType");
            }
        }

        if (StringUtils.hasText(condition.getConditionType())) {
            if (CONDITION_TYPE_NATIVE.equals(condition.getConditionType())) {
                validateNativeMode(condition);
            } else if (CONDITION_TYPE_CUSTOM.equals(condition.getConditionType())) {
                validateCustomMode(condition);
            }
        }
    }

    private void validateNativeMode(ProcessGatewayCondition condition) {
        if (!StringUtils.hasText(condition.getNativeExpression())) {
            throw new IllegalArgumentException("NATIVE模式必须填写nativeExpression");
        }
    }

    private void validateCustomMode(ProcessGatewayCondition condition) {
        if (CollectionUtils.isEmpty(condition.getRuleTree())) {
            throw new IllegalArgumentException("CUSTOM模式必须设置ruleTree");
        }
        for (ProcessGatewayConditionNode node : condition.getRuleTree()) {
            validateRuleTreeNode(node);
        }
    }

    private void validateRuleTreeNode(ProcessGatewayConditionNode node) {
        if (node == null) {
            return;
        }

        if (!StringUtils.hasText(node.getNodeType())) {
            throw new IllegalArgumentException("规则树节点nodeType不能为空");
        }

        String nodeType = node.getNodeType();

        if (NODE_TYPE_AND.equals(nodeType) || NODE_TYPE_OR.equals(nodeType)) {
            if (CollectionUtils.isEmpty(node.getChildren())) {
                throw new IllegalArgumentException("AND/OR节点必须包含子节点");
            }
            for (ProcessGatewayConditionNode child : node.getChildren()) {
                validateRuleTreeNode(child);
            }
        } else if (NODE_TYPE_CONDITION.equals(nodeType)) {
            validateConditionNode(node);
        }
    }

    private void validateConditionNode(ProcessGatewayConditionNode node) {
        if (!StringUtils.hasText(node.getCategory())) {
            throw new IllegalArgumentException("CONDITION节点category不能为空");
        }
        if (!StringUtils.hasText(node.getFieldKey())) {
            throw new IllegalArgumentException("CONDITION节点fieldKey不能为空");
        }
        if (!StringUtils.hasText(node.getOperator())) {
            throw new IllegalArgumentException("CONDITION节点operator不能为空");
        }
        if (!StringUtils.hasText(node.getValue())) {
            throw new IllegalArgumentException("CONDITION节点value不能为空");
        }

        if (OPERATOR_IN.equals(node.getOperator()) || OPERATOR_NOT_IN.equals(node.getOperator())) {
            validateJsonArrayValue(node.getValue());
        }
    }

    private void validateJsonArrayValue(String value) {
        try {
            Object parsed = objectMapper.readValue(value, Object.class);
            if (!(parsed instanceof List) && !(parsed instanceof Object[])) {
                throw new IllegalArgumentException("IN/NOT_IN的value必须是有效的JSON数组字符串");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("IN/NOT_IN的value必须是有效的JSON数组字符串");
        }
    }

}