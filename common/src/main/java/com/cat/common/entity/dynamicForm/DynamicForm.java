package com.cat.common.entity.dynamicForm;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;
import java.io.Serial;
import java.util.List;

/**
 * <p>
 * 动态表单
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-05-02
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_dynamic_form")
@Schema(name = "DynamicForm", description = "动态表单")
public class DynamicForm implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "表单id")
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @Schema(description = "表单名称")
    private String name;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "版本")
    private String version;

    @Schema(description = "状态 0 草稿 1 发布 -1 停用")
    private String status;

    @Schema(description = "逻辑删除")
    @TableLogic
    private String deleted;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建人昵称")
    @TableField(exist = false)
    private String createByName;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime updateTime;



    @Schema(description = "表单项信息")
    @TableField(exist = false)
    private List<DynamicFormField> formFields;

    @Schema(description = "字段分组")
    @TableField(exist = false)
    private List<DynamicFormFieldGroup> groups;

    @Schema(description = "联动规则")
    @TableField(exist = false)
    private List<DynamicFormLinkageRule> linkageRules;


}