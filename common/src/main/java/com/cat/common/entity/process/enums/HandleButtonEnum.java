package com.cat.common.entity.process.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum HandleButtonEnum {

    APPLY("0","申请"),
    PASS("1","通过"),
    REJECT("2","拒绝"),
    TRANSFER("3","转办"),
    DELEGATE("4","委派"),
    ADD_SIGN("5","加签"),
    RETURN("6","退回"),
    COPY("7","抄送"),
    SYSTEM_TASK("8","系统任务");


    private final String code;

    private final String name;








}
