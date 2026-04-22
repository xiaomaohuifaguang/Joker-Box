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
 * 干大事帖子评论
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-04-18
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_gan_da_shi_comment")
@Schema(name = "GanDaShiComment", description = "干大事帖子评论")
public class GanDaShiComment implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "评论id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "帖子id")
    private String postId;

    @Schema(description = "主评论id")
    private String parentId;

    @Schema(description = "回复id")
    private String replayId;

    @Schema(description = "回复评论人名称")
    @TableField(exist = false)
    private String replayName;

    @Schema(description = "评论")
    private String comment;

    @Schema(description = "逻辑删除")
    @TableLogic
    private String deleted;

    @Schema(description = "创建人id")
    private String createBy;

    @Schema(description = "创建人昵称")
    @TableField(exist = false)
    private String createByName;

    @Schema(description = "回复时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "回复数量")
    private Integer replayCount = 0;

}