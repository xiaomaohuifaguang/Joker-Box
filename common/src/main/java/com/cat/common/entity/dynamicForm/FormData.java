package com.cat.common.entity.dynamicForm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "FormData", description = "表单数据提交")
public class FormData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "表单id")
    private String formId;

    @Schema(description = "版本")
    private String version;

    @Schema(description = "表单实例")
    private String formInstanceId;

    @Schema(description = "数据")
    private HashMap<String, Object> data;



}
