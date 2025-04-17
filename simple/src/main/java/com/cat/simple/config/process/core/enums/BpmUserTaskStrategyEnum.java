package com.cat.simple.config.process.core.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BpmUserTaskStrategyEnum {


    ROLE("10", "角色"),
    DEPT_MEMBER("20", "部门的成员"), // 包括负责人
    DEPT_LEADER("21", "部门的负责人"),
    MULTI_DEPT_LEADER_MULTI("23", "连续多级部门的负责人"),
    POST("22", "岗位"),
    USER("30", "用户"),
    START_USER_SELECT("35", "发起人自选"), // 申请人自己，可在提交申请时选择此节点的审批人
    START_USER("36", "发起人自己"), // 申请人自己, 一般紧挨开始节点，常用于发起人信息审核场景
    START_USER_DEPT_LEADER("37", "发起人部门负责人"),
    START_USER_DEPT_LEADER_MULTI("38", "发起人连续多级部门的负责人"),
    USER_GROUP("40", "用户组"),
    FORM_USER("50", "表单内用户字段"),
    FORM_DEPT_LEADER("51", "表单内部门负责人"),
    EXPRESSION("60", "流程表达式"), // 表达式 ExpressionManager
    ASSIGN_EMPTY("1", "审批人为空"),
    ;


    /**
     * 类型
     */
    private final String strategy;
    /**
     * 描述
     */
    private final String description;

}
