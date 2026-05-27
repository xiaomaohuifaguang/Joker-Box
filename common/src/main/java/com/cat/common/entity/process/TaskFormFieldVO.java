package com.cat.common.entity.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "任务表单字段渲染数据")
public class TaskFormFieldVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "字段标识")
    private String fieldKey;

    @Schema(description = "字段标题")
    private String label;

    @Schema(description = "字段类型")
    private String type;

    @Schema(description = "字段权限：VISIBLE/READONLY/HIDDEN/EDITABLE/REQUIRED")
    private String permission;

    @Schema(description = "当前值")
    private Object value;

    @Schema(description = "是否必填")
    private boolean required;

    @Schema(description = "选项列表")
    private List<?> options;

    @Schema(description = "来源表单ID（仅继承字段有值）")
    private String sourceFormId;
}
