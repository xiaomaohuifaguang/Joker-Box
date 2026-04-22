package com.cat.common.entity.ai.model;

import com.baomidou.mybatisplus.annotation.IdType;
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
 * 
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-12-20
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_ai_model")
@Schema(name = "AiModel", description = "")
public class AiModel implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "id")
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "模型")
    private String model;

    @Schema(description = "版本")
    private String version;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "创建人")
    private String userId;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime createTime;
}