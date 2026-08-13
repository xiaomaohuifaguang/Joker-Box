package com.cat.simple.config.flowable.gateway;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.dynamicForm.DynamicFormField;
import com.cat.common.entity.process.NodeFieldCategory;
import com.cat.common.entity.process.ProcessGatewayCondition;
import com.cat.common.entity.process.ProcessGatewayConditionNode;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.gateway.operator.ConditionOperator;
import com.cat.simple.config.flowable.gateway.operator.OperatorFactory;
import com.cat.simple.process.mapper.ProcessGatewayConditionMapper;
import com.cat.simple.process.mapper.ProcessGatewayConditionNodeMapper;
import com.cat.simple.process.service.ProcessFormService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class GatewayConditionEngine {

    @Resource
    private ProcessGatewayConditionMapper conditionMapper;
    @Resource
    private ProcessGatewayConditionNodeMapper nodeMapper;
    @Resource
    private OperatorFactory operatorFactory;
    @Resource
    private ProcessFormService processFormService;

    public boolean evaluate(Integer processDefinitionId, String version,
                            String sequenceFlowId, ProcessInstance instance) {
        ProcessGatewayCondition condition = conditionMapper.selectOne(
                new LambdaQueryWrapper<ProcessGatewayCondition>()
                        .eq(ProcessGatewayCondition::getProcessDefinitionId, processDefinitionId)
                        .eq(ProcessGatewayCondition::getVersion, version)
                        .eq(ProcessGatewayCondition::getSequenceFlowId, sequenceFlowId)
                        .eq(ProcessGatewayCondition::getConditionType, "CUSTOM"));

        if (condition == null || Boolean.TRUE.equals(condition.getIsDefault())) {
            return false;
        }
        if (!"CUSTOM".equals(condition.getConditionType())) {
            return false;
        }

        List<ProcessGatewayConditionNode> ruleTree = loadRuleTree(condition.getId());
        if (CollectionUtils.isEmpty(ruleTree)) {
            return true;
        }

        List<DynamicFormField> globalFields = processFormService.getGlobalFields(instance.getId());
        Map<String, DynamicFormField> globalFormData = globalFields.stream()
                .collect(Collectors.toMap(
                        DynamicFormField::getFieldId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
        return evaluateNode(ruleTree.get(0), globalFormData);
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

    private boolean evaluateNode(ProcessGatewayConditionNode node, Map<String, DynamicFormField> globalFormData) {
        if (node == null) return true;

        return switch (node.getNodeType()) {
            case "AND" -> {
                if (CollectionUtils.isEmpty(node.getChildren())) yield true;
                yield node.getChildren().stream().allMatch(child-> evaluateNode(child, globalFormData));
            }
            case "OR" -> {
                if (CollectionUtils.isEmpty(node.getChildren())) yield true;
                yield node.getChildren().stream().anyMatch(child-> evaluateNode(child, globalFormData));
            }
            case "CONDITION" -> evaluateCondition(node, globalFormData);
            default -> true;
        };
    }

    private boolean evaluateCondition(ProcessGatewayConditionNode node, Map<String, DynamicFormField> globalFormData) {

        Object actualValue = null;
        if(node.getCategory().equals(NodeFieldCategory.FORM_FIELD)){
            DynamicFormField dynamicFormField = globalFormData.get(node.getFieldKey());
            actualValue = dynamicFormField.getValue();
        }

        ConditionOperator operator = operatorFactory.get(node.getOperator());

        return operator.compare(actualValue, node.getValue());
    }
}
