package com.cat.common.entity.ganDaShi;

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
 * 干大事主贴
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-04-18
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_gan_da_shi_post")
@Schema(name = "GanDaShiPost", description = "干大事主贴")
public class GanDaShiPost implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "干大事主贴id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "纯文字")
    private String text;

    @Schema(description = "摘要")
    private String digest;

    @Schema(description = "逻辑删除")
    @TableLogic
    private String deleted;

    @Schema(description = "创建人用户名")
    @TableField(exist = false)
    private String createUsername;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建人昵称")
    @TableField(exist = false)
    private String createByName;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "浏览量")
    private Integer viewCount;


}