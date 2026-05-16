package com.cat.simple.config.flowable.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum MultiInstanceBackPolicyEnum {

    AUTO("auto", "智能默认：会签→全体回退，或签/随机/认领→独立处理"),
    ALL_BACK("all_back", "一人 back，取消所有并行实例，整体回退"),
    INDEPENDENT("independent", "仅处理当前子实例，其他实例不受影响");

    private final String code;

    private final String description;

    public static MultiInstanceBackPolicyEnum of(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElse(null);
    }
}
