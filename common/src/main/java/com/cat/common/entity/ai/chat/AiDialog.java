package com.cat.common.entity.ai.chat;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
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
 * @since 2024-08-02
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_ai_dialog")
@Schema(name = "AiDialog", description = "会话信息")
public class AiDialog implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "会话id")
    @TableId(value = "id")
    private String id;

    @Schema(description = "名称/摘要")
    private String name;

    @Schema(description = "创建人")
    private String userId;

    @Schema(description = "逻辑删除 1 是 0 否")
    @TableLogic
    private String deleted = "0";

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime createTime = LocalDateTime.now();
}