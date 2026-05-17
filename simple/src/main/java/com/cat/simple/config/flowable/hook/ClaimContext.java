package com.cat.simple.config.flowable.hook;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 认领操作上下文，封装认领请求参数。
 */
@Data
@AllArgsConstructor
public class ClaimContext {
    private Integer processInstanceId;
    private String taskId;
    private String remark;
}
