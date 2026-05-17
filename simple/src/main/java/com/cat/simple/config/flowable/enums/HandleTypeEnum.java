package com.cat.simple.config.flowable.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程操作类型枚举，记录用户对流程实例的处理动作。
 */
@AllArgsConstructor
@Getter
public enum HandleTypeEnum {

    APPLY("apply","申请"),
    PASS("pass","通过"),
    REJECT("reject","拒绝"),
    TRANSFER("transfter","转办"),
    DELEGATE("delegate","委派"),
    ADD_SIGN("add_sign","加签"),
    BACK("back","驳回"),
    COPY("copy","抄送"),
    CLAIM("claim","认领");


    private final String code;

    private final String name;








}
