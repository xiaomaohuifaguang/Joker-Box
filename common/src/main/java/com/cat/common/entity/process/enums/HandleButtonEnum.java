package com.cat.common.entity.process.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum HandleButtonEnum {

    APPLY("apply","申请"),
    PASS("pass","通过"),
    REJECT("reject","拒绝"),
    TRANSFER("transfter","转办"),
    DELEGATE("delegate","委派"),
    ADD_SIGN("add_sign","加签"),
    BACK("back","驳回"),
    COPY("copy","抄送"),
    SYSTEM_TASK("system_task","系统任务"),
    CLAIM("claim","认领");


    private final String code;

    private final String name;








}
