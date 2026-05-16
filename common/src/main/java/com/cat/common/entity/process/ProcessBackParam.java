package com.cat.common.entity.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(name = "ProcessBackParam", description = "流程驳回参数")
public class ProcessBackParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "自建流程实例id")
    private Integer processInstanceId;

    @Schema(description = "Flowable任务id")
    private String taskId;

    @Schema(description = "备注/驳回意见")
    private String remark;

    @Schema(description = "目标节点id（backType=choose时必填）")
    private String targetNodeId;
}
