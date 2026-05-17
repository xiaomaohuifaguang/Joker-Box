package com.cat.simple.config.flowable.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 驳回类型枚举，定义回退目标节点的选择方式。
 */
@AllArgsConstructor
@Getter
public enum BackTypeEnum {

    PREV("prev", "上一节点"),
    SPECIFIC("specific", "驳回到指定节点"),
    CHOOSE("choose","用户自选");

    private final String code;

    private final String name;

    /** 按编码解析枚举，不存在返回 null。 */
    public static BackTypeEnum of(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElse(null);
    }

}
