package com.cat.common.entity.codeTable;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 码表选项
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2026-05-24
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "CodeOption", description = "码表选项")
public class CodeOption implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "标签")
    private String label;

    @Schema(description = "值")
    private String value;

    @Schema(description = "子级")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<CodeOption> children;

}
