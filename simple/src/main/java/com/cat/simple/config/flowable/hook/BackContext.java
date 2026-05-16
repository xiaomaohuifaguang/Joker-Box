package com.cat.simple.config.flowable.hook;

import com.cat.common.entity.process.BackConfig;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BackContext {
    private Integer processInstanceId;
    private String taskId;
    private String remark;
    private String targetNodeId;
    private BackConfig backConfig;
}
