package com.cat.common.entity.workOrder;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.cat.common.entity.process.ProcessInfo;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;
import java.io.Serial;
import java.util.List;


/**
 * <p>
 * 工单主要信息
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-04-12
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_work_order")
@Schema(name = "WorkOrder", description = "工单主要信息")
public class WorkOrder implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工单id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "工单号")
    private String orderNo;

    @Schema(description = "流程定义id")
    private Integer processDefinitionId;

    @Schema(description = "流程实例id")
    private Integer processInstanceId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态 0 草稿 1 提交")
    private String status;

    @Schema(description = "流程状态")
    @TableField(exist = false)
    private String processStatus;

    @Schema(description = "任务id")
    @TableField(exist = false)
    private String taskId;

    @Schema(description = "节点名称")
    @TableField(exist = false)
    private List<String> taskNames;

    @Schema(description = "逻辑删除")
    @TableLogic
    private String deleted;

    @Schema(description = "申请人")
    private String createBy;

    @Schema(description = "申请人昵称")
    @TableField(exist = false)
    private String createByName;


    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime createTime = LocalDateTime.now();

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime updateTime = LocalDateTime.now();

    @Schema(description = "流程创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    @TableField(exist = false)
    private LocalDateTime processInstanceCreateTime;

    @Schema(description = "流程更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    @TableField(exist = false)
    private LocalDateTime processInstanceUpdateTime;

    @Schema(description = "流程定义名称")
    @TableField(exist = false)
    private String processDefinitionName;

    @Schema(description = "流程定义名称")
    @TableField(exist = false)
    private ProcessInfo processInfo;



}