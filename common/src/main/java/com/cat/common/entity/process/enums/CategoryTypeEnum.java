package com.cat.common.entity.process.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum CategoryTypeEnum {

    // name 命名规范要求 组织名称-服务类型
    DEFAULT("default", "默认分类"),
    OA("oa","OA");

    private final String type;

    private final String name;

    CategoryTypeEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }


    /** 按编码解析枚举，不存在返回 null。 */
    public static CategoryTypeEnum of(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.type.equals(type))
                .findFirst()
                .orElse(null);
    }

}
