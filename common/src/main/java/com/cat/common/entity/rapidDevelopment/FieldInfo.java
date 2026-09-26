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
    private String field;

    @Schema(description = "字段名小驼峰")
    private String fieldName;

    @Schema(description = "类型")
    private String Type;

    @Schema(description = "java类型")
    private String javaType;

    @Schema(description = "允许为空")
    private String Null;

    @Schema(description = "Null == YES")
    private Boolean nullable;

    @Schema(description = "键")
    private String Key;

    private String extra;

    @Schema(description = "默认值")
    private String Default;

    @Schema(description = "注释")
    private String comment;

    @Schema(description = "ts类型")
    private String tsType;



    /** 是否主键 */
    @Schema(description = "是否主键")
    private Boolean keyFlag;
    /** 是否自增主键 */
    @Schema(description = "是否自增主键")
    private Boolean keyIdentityFlag;
    /** 是否需要 @TableField 显式映射（列名≠驼峰属性名）*/
    @Schema(description = "是否需要 @TableField 显式映射（列名≠驼峰属性名）")
    private Boolean convert;





}
