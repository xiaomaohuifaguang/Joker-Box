package com.cat.common.entity.codeTable;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 码表项
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2026-05-24
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_code_item")
@Schema(name = "CodeItem", description = "码表项")
public class CodeItem implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "码表项id")
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @Schema(description = "码表id")
    private String tableId;

    @Schema(description = "父级id")
    private String parentId;

    @Schema(description = "标签")
    private String label;

    @Schema(description = "值")
    private String value;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态 0 停用 1 启用")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "逻辑删除")
    @TableLogic
    private String deleted;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
