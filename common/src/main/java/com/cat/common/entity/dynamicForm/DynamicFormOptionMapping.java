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
@Schema(name = "DynamicFormOptionMapping", description = "动态表单远程选项映射配置")
public class DynamicFormOptionMapping implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "候选项数组路径，响应根数组使用 $")
    private String listPath;

    @Schema(description = "选项显示文本字段路径")
    private String labelPath;

    @Schema(description = "选项值字段路径")
    private String valuePath;

    @Schema(description = "子选项字段路径")
    private String childrenPath;
}
