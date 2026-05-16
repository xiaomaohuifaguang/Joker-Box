package com.cat.simple.config.flowable.constant;

import com.cat.simple.config.flowable.enums.ExtensionElementEnum;

public class ProcessConstants {


    /** BPMN 扩展元素名：审批类型 */
    public static final String EL_APPROVAL_TYPE = ExtensionElementEnum.APPROVAL_TYPE.getCode();
    /** BPMN 扩展元素名：候选用户 */
    public static final String EL_CANDIDATE_USERS = ExtensionElementEnum.CANDIDATE_USERS.getCode();
    /** BPMN 扩展元素名：候选角色 */
    public static final String EL_CANDIDATE_ROLES = ExtensionElementEnum.CANDIDATE_ROLES.getCode();
    /** BPMN 扩展元素名：候选组 */
    public static final String EL_CANDIDATE_GROUPS = ExtensionElementEnum.CANDIDATE_GROUPS.getCode();
    /** BPMN 扩展元素名：候选部门 */
    public static final String EL_CANDIDATE_DEPTS = ExtensionElementEnum.CANDIDATE_DEPTS.getCode();
    /** BPMN 扩展元素名：通过率 */
    public static final String EL_PASS_RATE = ExtensionElementEnum.PASS_RATE.getCode();
    /** BPMN 扩展元素名：处理按钮 */
    public static final String EL_ACTION_BUTTONS = ExtensionElementEnum.ACTION_BUTTONS.getCode();
    /** BPMN 扩展元素名：驳回方式 */
    public static final String EL_BACK_TYPE = ExtensionElementEnum.BACK_TYPE.getCode();
    /** BPMN 扩展元素名：驳回节点 */
    public static final String EL_BACK_NODE_ID = ExtensionElementEnum.BACK_NODE_ID.getCode();
    /** BPMN 扩展元素名：回退后任务分配策略 */
    public static final String EL_BACK_ASSIGNEE_POLICY = ExtensionElementEnum.BACK_ASSIGNEE_POLICY.getCode();

    /** 回退时注入流程变量的前缀，用于临时覆盖多实例 collection */
    public static final String BACK_ASSIGNEE_VAR_PREFIX = "__back_assignees_";

}
