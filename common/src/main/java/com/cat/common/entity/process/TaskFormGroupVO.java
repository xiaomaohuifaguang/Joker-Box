package com.cat.common.entity.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "任务表单分组渲染数据")
public class TaskFormGroupVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "分组ID")
    private String groupId;

    @Schema(description = "分组名称")
    private String name;

    @Schema(description = "分组描述")
    private String description;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "默认折叠 0展开 1折叠")
    private String collapsed;

    @Schema(description = "分组下的字段列表")
    private List<TaskFormFieldVO> fields;
}