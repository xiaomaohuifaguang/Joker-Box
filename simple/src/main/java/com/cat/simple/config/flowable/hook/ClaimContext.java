package com.cat.simple.config.flowable.hook;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClaimContext {
    private Integer processInstanceId;
    private String taskId;
    private String remark;
}
