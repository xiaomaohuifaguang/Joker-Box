package ${entityPackage};

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.io.Serial;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;


<#list entityImportPackages as pkg>
import ${pkg};
</#list>


@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(description = "${tableComment}")
@JsonInclude(JsonInclude.Include.NON_NULL)
@TableName("${tableName}")
public class ${tableNameUp} implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
<#list fieldInfos as field>
    @Schema(description = "${field.comment}")
    <#-- 每个字段的注解生成 -->
    <#if field.keyFlag>
    <#-- 主键 -->
        <#if field.keyIdentityFlag>
    @TableId(value = "${field.field}", type = IdType.AUTO)
        <#else>
    @TableId("${field.field}")
        </#if>
    <#elseif field.convert>
    @TableField("${field.field}")
    </#if>
    <#if field.javaType == "LocalDateTime" || field.javaType == "Date">
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    </#if>
    private ${field.javaType} ${field.fieldName};
</#list>

}