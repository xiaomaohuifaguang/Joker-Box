package com.cat.simple.config.flowable.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BackTypeEnum {

    PREV("prev", "上一节点"),
    SPECIFIC("specific", "驳回到指定节点"),
    CHOOSE("choose","用户自选");

    private final String code;

    private final String name;

}
