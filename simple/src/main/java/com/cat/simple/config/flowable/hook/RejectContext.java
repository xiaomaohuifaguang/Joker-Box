package com.cat.simple.config.flowable.hook;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 拒绝操作上下文，封装拒绝请求参数。
 */
@Data
@AllArgsConstructor
public class RejectContext {
    private Integer processInstanceId;
    private String taskId;
    private String remark;
}
