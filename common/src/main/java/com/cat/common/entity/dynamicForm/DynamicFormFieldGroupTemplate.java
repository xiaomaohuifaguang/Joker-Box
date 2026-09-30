package com.cat.common.entity.dynamicForm;

import com.baomidou.mybatisplus.annotation.IdType;
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



@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(description = "cat_dynamic_form_field__group_template")
@JsonInclude(JsonInclude.Include.NON_NULL)
@TableName("cat_dynamic_form_field__group_template")
public class DynamicFormFieldGroupTemplate implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @Schema(description = "字段组模板id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "字段模板集")
    @TableField("fields_template")
    private String fieldsTemplate;
    @Schema(description = "联动规则")
    @TableField("linkage_rules")
    private String linkageRules;

}