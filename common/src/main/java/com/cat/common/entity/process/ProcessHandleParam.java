package com.cat.common.entity.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * <p>
 * 流程任务操作参数（认领 / 通过 / 拒绝等通用）
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-04-13
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "ProcessHandleParam", description = "流程任务操作参数")
public class ProcessHandleParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "自建流程实例id")
    private Integer processInstanceId;

    @Schema(description = "Flowable任务id")
    private String taskId;

    @Schema(description = "备注/审批意见")
    private String remark;

}