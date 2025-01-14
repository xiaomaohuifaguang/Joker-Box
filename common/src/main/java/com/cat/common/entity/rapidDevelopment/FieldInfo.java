package com.cat.common.entity.rapidDevelopment;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "FieldInfo", description = "字段信息")
public class FieldInfo {

    @Schema(description = "字段名")
    private String Field;

    @Schema(description = "字段名小驼峰")
    private String fieldName;

    @Schema(description = "类型")
    private String Type;

    @Schema(description = "允许为空")
    private String Null;

    @Schema(description = "键")
    private String Key;

    @Schema(description = "默认值")
    private String Default;

    @Schema(description = "注释")
    private String Comment;





}
