package com.cat.common.entity.codeTable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * <p>
 * 码表项查询参数
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2026-05-24
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "CodeItemQueryParam", description = "码表项查询参数")
public class CodeItemQueryParam implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "码表id")
    private String tableId;

    @Schema(description = "父级id")
    private String parentId;

    @Schema(description = "标签")
    private String label;

    @Schema(description = "值")
    private String value;

    @Schema(description = "状态 0 停用 1 启用")
    private String status;

}
