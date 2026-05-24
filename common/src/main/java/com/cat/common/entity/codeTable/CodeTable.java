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
 * 码表
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2026-05-24
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_code_table")
@Schema(name = "CodeTable", description = "码表")
public class CodeTable implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "码表id")
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @Schema(description = "码表编码")
    private String code;

    @Schema(description = "码表名称")
    private String name;

    @Schema(description = "是否树形 0 否 1 是")
    private String tree;

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
