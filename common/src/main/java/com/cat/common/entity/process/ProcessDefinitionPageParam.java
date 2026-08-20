package com.cat.common.entity.process;

import com.cat.common.entity.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "ProcessDefinitionPageParam", description = "流程定义分页查询参数")
@EqualsAndHashCode(callSuper = false)
public class ProcessDefinitionPageParam extends PageParam {

    @Schema(description = "流程分类")
    private String processCategory;


}
