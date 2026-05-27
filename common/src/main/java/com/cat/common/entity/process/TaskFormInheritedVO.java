package com.cat.common.entity.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "继承表单渲染数据")
public class TaskFormInheritedVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主表单ID")
    private String formId;

    @Schema(description = "主表单版本")
    private String formVersion;

    @Schema(description = "主表单实例ID")
    private String formInstanceId;

    @Schema(description = "未分组字段列表")
    private List<TaskFormFieldVO> formFields;

    @Schema(description = "分组字段列表")
    private List<TaskFormGroupVO> groups;
}