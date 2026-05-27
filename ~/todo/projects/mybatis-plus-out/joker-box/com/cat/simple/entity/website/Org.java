package com.cat.simple.entity.website;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
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
 * @since 2024-11-27
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

    @Schema(description = "机构名称")
    private String name;

    @Schema(description = "逻辑删除")
    private String deleted;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}