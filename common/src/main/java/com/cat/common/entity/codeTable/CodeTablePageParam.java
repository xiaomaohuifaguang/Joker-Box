package com.cat.common.entity.codeTable;

import com.cat.common.entity.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * <p>
 * 码表分页查询参数
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2026-05-24
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(name = "CodeTablePageParam", description = "码表分页查询参数")
public class CodeTablePageParam extends PageParam implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "码表编码")
    private String code;

    @Schema(description = "码表名称")
    private String name;

    @Schema(description = "是否树形 0 否 1 是")
    private String tree;

    @Schema(description = "状态 0 停用 1 启用")
    private String status;

}
