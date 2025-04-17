package com.cat.simple.config.process.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BpmUserTaskApproveMethodEnum {

    RANDOM_SIGN("1", "随机挑选一人审批", null),
    RATIO_SIGN("2", "多人会签(按通过比例)", "${ nrOfCompletedInstances/nrOfInstances >= %s}"), // 会签（按通过比例）
    ANY_SIGN("3", "多人或签(一人通过或拒绝)", "${ nrOfCompletedInstances > 0 }"), // 或签（通过只需一人，拒绝只需一人）
    SEQUENTIAL_SIGN("4", "依次审批", "${ nrOfCompletedInstances >= nrOfInstances }"); // 依次审批

    /**
     * 审批方式
     */
    private final String method;
    /**
     * 名字
     */
    private final String name;
    /**
     * 完成表达式
     */
    private final String completionCondition;

}
