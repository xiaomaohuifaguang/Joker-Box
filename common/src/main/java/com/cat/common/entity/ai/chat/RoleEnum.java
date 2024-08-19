package com.cat.common.entity.ai.chat;

/***
 * <TODO description class purpose>
 * @title RoleEnum
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/8/2 22:24
 **/
public enum RoleEnum {

    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool"),
    ;

    RoleEnum(String value) {
        this.value = value;
    }

    private final String value;

    public String value(){
        return this.value;
    }

}
