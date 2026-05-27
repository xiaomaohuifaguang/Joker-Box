package com.cat.common.entity.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "保存草稿参数")
public class SaveDraftParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "草稿流程实例id，传则更新，不传则新建")
    private Integer id;

    @Schema(description = "自建流程定义id", required = true)
    private Integer processDefinitionId;

    @Schema(description = "流程标题")
    private String title;

    @Schema(description = "表单数据")
    private Map<String, Object> formData;
}