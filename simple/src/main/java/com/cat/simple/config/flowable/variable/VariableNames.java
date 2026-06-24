package com.cat.simple.config.flowable.variable;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程变量名枚举，统一封装 BPMN 扩展元素与运行时变量的 key。
 */
@AllArgsConstructor
@Getter
public enum VariableNames {
    APPROVAL_TYPE("approvalType"),
    CANDIDATE_USERS("candidateUsers"),
    CANDIDATE_ROLES("candidateRoles"),
    CANDIDATE_GROUPS("candidateGroups"),
    CANDIDATE_DEPTS("candidateDepts"),
    PASS_RATE("passRate"),
    ACTION_BUTTONS("actionButtons"),
    BACK_TYPE("backType"),
    BACK_NODE_ID("backNodeId"),
    BACK_ASSIGNEE_POLICY("backAssigneePolicy"),
    FORM_DATA("formData"),
    BACK_ASSIGNEES_PREFIX("__back_assignees_"),
    GATEWAY_RESULTS("gatewayResults"),
    HANDLER_DEPT("__handler_dept"),
    HANDLER_ROLE("__handler_role"),
    PREV_HANDLER_DEPT("__prev_handler_dept"),
    PREV_HANDLER_ROLE("__prev_handler_role");

    private final String key;
}
