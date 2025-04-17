package com.cat.common.entity.process;

import com.cat.common.entity.process.enums.HandleButtonEnum;

import java.util.List;

public class ProcessConstants {


    public static final List<String> handleButtons = List.of(
            HandleButtonEnum.PASS.getCode(),
            HandleButtonEnum.REJECT.getCode(),
            HandleButtonEnum.TRANSFER.getCode(),
            HandleButtonEnum.RETURN.getCode()
    );




}
