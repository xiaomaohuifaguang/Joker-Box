package com.cat.common.entity.auth;

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
 * 组织机构表
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2024-11-29
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_org")
@Schema(name = "Org", description = "组织机构表")
public class Org implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "组织id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "父级机构id")
    private Integer parentId;

    @Schema(description = "父级机构名称")
    @TableField(exist = false)
    private String parentName;

    @Schema(description = "机构名称")
    private String name;

    @Schema(description = "逻辑删除")
    @TableLogic
    private String deleted;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime updateTime;
}