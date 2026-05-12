package com.cat.common.entity.process.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ProcessStatusEnum {

    DRAFT("0","草稿"),
    ACTIVE("1","审批中"),
    TODO("21","待办"), // 不存入库
    COMPLETED("10","已完成"),
    SUSPENDED("20","已挂起"),
    TERMINATED("11", "已终止"),
    UNKNOWN("", "未知");


    private final String status;

    private final String description;







}
