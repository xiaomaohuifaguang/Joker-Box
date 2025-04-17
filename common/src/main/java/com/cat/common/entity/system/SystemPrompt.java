package com.cat.common.entity.system;

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
 * 系统提示
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-04-16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_system_prompt")
@Schema(name = "SystemPrompt", description = "系统提示")
public class SystemPrompt implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "系统提示id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "系统提示消息")
    private String prompt;

    @Schema(description = "逻辑删除")
    @TableLogic
    private String deleted;

    @Schema(description = "创建人id")
    private String createBy;

    @Schema(description = "创建人名称")
    @TableField(exist = false)
    private String createByName;


    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "截止时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime deadTime;
}