package com.cat.common.entity.dynamicForm;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.extension.handlers.Fastjson2TypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;
import java.io.Serial;
/**
 * <p>
 * 表单项实例id
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-05-06
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_dynamic_form_field_instance")
@Schema(name = "DynamicFormFieldInstance", description = "表单项实例id")
public class DynamicFormFieldInstance implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "表单项实例id")
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @Schema(description = "表单项id")
    private String formFieldId;

    @Schema(description = "表单实例id")
    private String formInstanceId;

    @Schema(description = "真实值 包括不限于 输入框 选择框 开关等")
    @TableField(typeHandler = Fastjson2TypeHandler.class)
    private Object val;

    @Schema(description = "版本")
    private String version;

    @Schema(description = "逻辑删除")
    @TableLogic
    private String deleted;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime updateTime;








}