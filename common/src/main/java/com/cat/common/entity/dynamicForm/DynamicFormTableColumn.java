package com.cat.common.entity.dynamicForm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "DynamicFormTableColumn", description = "动态表格列定义")
public class DynamicFormTableColumn implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "列标识")
    private String key;

    @Schema(description = "列标题")
    private String title;
}
