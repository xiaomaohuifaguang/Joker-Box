package com.cat.common.entity.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "任务表单渲染数据")
public class TaskFormVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "当前用户是否可编辑（是否为任务处理人）")
    private boolean editable;

    @Schema(description = "当前节点绑定的表单ID")
    private String formId;

    @Schema(description = "表单版本")
    private String formVersion;

    @Schema(description = "表单实例ID")
    private String formInstanceId;

    @Schema(description = "未分组字段列表")
    private List<TaskFormFieldVO> formFields;

    @Schema(description = "分组字段列表")
    private List<TaskFormGroupVO> groups;

    @Schema(description = "继承的主表单数据（仅 inheritMainForm=1 时有值）")
    private TaskFormInheritedVO inherited;
}