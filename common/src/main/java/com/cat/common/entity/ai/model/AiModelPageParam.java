package com.cat.common.entity.ai.model;

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
@Schema(name = "AiModelPageParam", description = "模型管理分页参数")
@EqualsAndHashCode(callSuper = false)
public class AiModelPageParam extends PageParam {
    @Schema(description = "类型")
    String type;


}
