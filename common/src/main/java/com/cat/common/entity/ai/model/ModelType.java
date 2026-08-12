package com.cat.common.entity.ai.model;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ModelType {

    CHAT("CHAT", "对话模型"),
    EMBEDDING("EMBEDDING", "向量模型");

    ModelType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    private final String code;
    private final String desc;

    public static ModelType of(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElse(null);
    }


}
