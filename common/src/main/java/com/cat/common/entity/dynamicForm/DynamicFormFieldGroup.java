package com.cat.common.entity.dynamicForm;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 动态表单字段分组
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-05-09
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_dynamic_form_field_group")
@Schema(name = "DynamicFormFieldGroup", description = "动态表单字段分组")
public class DynamicFormFieldGroup implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "分组id")
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @Schema(description = "表单id")
    private String formId;

    @Schema(description = "版本")
    private String version;

    @Schema(description = "分组名称")
    private String name;

    @Schema(description = "分组描述")
    private String description;

    @Schema(description = "分组排序，越小越靠前")
    private Integer sort;

    @Schema(description = "默认折叠 0展开 1折叠")
    private String collapsed;

    @Schema(description = "逻辑删除")
    @TableLogic
    private String deleted;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    @Schema(description = "分组下的字段")
    @TableField(exist = false)
    private List<DynamicFormField> fields;
}
