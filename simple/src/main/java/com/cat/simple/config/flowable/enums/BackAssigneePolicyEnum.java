package com.cat.simple.config.flowable.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum BackAssigneePolicyEnum {

    AUTO("auto", "智能默认：有上次办理人则派回，无则按配置重新分配"),
    LAST_HANDLER("last_handler", "派给上次办理人"),
    REASSIGN("reassign", "按节点 candidate 配置重新分配");

    private final String code;

    private final String description;

    public static BackAssigneePolicyEnum of(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElse(null);
    }
}
