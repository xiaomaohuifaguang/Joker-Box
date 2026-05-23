package com.cat.common.entity.dynamicForm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "DynamicFormOptionSource", description = "动态表单选项数据源配置")
public class DynamicFormOptionSource implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "选项来源类型：STATIC/API")
    private String type;

    @Schema(description = "API 地址，仅 API 模式使用")
    private String url;

    @Schema(description = "请求方式：GET/POST，仅 API 模式使用")
    private String method;

    @Schema(description = "静态请求参数")
    private Map<String, Object> params;

    @Schema(description = "API 响应映射配置")
    private DynamicFormOptionMapping mapping;
}
