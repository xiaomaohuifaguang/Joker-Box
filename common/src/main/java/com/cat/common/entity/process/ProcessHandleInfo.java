package com.cat.common.entity.process;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;
import java.io.Serial;
/**
 * <p>
 * 流程处理记录
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-04-13
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_process_handle_info")
@Schema(name = "ProcessHandleInfo", description = "流程处理记录")
public class ProcessHandleInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "流程处理记录")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "流程实例id")
    private Integer processInstanceId;

    @Schema(description = "任务id")
    private String taskId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "处理人id")
    private String handleUser;

    @Schema(description = "处理人名称")
    @TableField(exist = false)
    private String handleUserName;

    @Schema(description = "处理类型")
    private String handleType;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "处理时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime handleTime;
}