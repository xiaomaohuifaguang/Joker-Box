package com.cat.common.entity.process;

import com.cat.common.entity.dynamicForm.DynamicForm;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "任务表单渲染数据")
public class TaskFormVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "当前用户是否可编辑（是否为任务处理人）")
    private boolean editable;

    @Schema(description = "节点表单（含字段定义、权限、当前值、分组、联动规则）")
    private DynamicForm nodeForm;

    @Schema(description = "继承的全局表单（仅 inheritMainForm=1 且与节点表单不同时有值）")
    private DynamicForm globalForm;
}