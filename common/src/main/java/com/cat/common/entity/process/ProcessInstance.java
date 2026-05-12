package com.cat.common.entity.process;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;
import java.io.Serial;
/**
 * <p>
 * 流程实例表
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-03-06
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_process_instance")
@Schema(name = "ProcessInstance", description = "流程实例表")
public class ProcessInstance implements Serializable {


    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "流程实例")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "流程定义id = 自定义流程定义表对应id")
    private Integer processDefinitionId;

    @Schema(description = "流程标题")
    private String title;

    @Schema(description = "流程编号")
    private String code;

    @Schema(description = "流程定义名称")
    @TableField(exist = false)
    private String processDefinitionName;

    @Schema(description = "流程实例id = 流程引擎生成")
    private String processInstanceId;

    @Schema(description = "流程状态")
    private String processStatus = "0";

    @Schema(description = "逻辑删除")
    @TableLogic
    private String deleted = "0";

    @Schema(description = "创建人==申请人")
    private String createBy;

    @Schema(description = "创建人昵称")
    @TableField(exist = false)
    private String createByName;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime updateTime;

    @Schema(description = "任务id")
    @TableField(exist = false)
    private String taskId;



}