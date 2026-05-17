package com.cat.simple.config.flowable.hook;

import com.cat.common.entity.process.BackConfig;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 驳回操作上下文，封装驳回请求参数。
 */
@Data
@AllArgsConstructor
public class BackContext {
    private Integer processInstanceId;
    private String taskId;
    private String remark;
    private String targetNodeId;
    private BackConfig backConfig;
}
