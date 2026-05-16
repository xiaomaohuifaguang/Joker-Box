package com.cat.simple.config.flowable.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ExtensionElementEnum {

    APPROVAL_TYPE("approvalType", "审批类型"),
    CANDIDATE_USERS("candidateUsers", "候选用户"),
    CANDIDATE_ROLES("candidateRoles", "候选角色"),
    CANDIDATE_GROUPS("candidateGroups", "候选组"),
    CANDIDATE_DEPTS("candidateDepts", "候选部门"),
    PASS_RATE("passRate", "通过率"),
    ACTION_BUTTONS("actionButtons", "处理按钮"),
    BACK_TYPE("backType", "驳回方式"),
    BACK_NODE_ID("backNodeId", "驳回节点"),
    BACK_ASSIGNEE_POLICY("backAssigneePolicy", "回退后任务分配策略");

    private final String code;

    private final String description;

}
