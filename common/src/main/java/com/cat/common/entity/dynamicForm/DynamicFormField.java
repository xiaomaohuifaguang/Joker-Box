package com.cat.common.entity.dynamicForm;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.extension.handlers.Fastjson2TypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-04-30
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName(value = "cat_dynamic_form_field",autoResultMap = true)
@Schema(name = "DynamicFormField", description = "表单项")
public class DynamicFormField implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "表单项id")
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @Schema(description = "前端设计id")
    private String fieldId;

    @Schema(description = "表单id")
    private String formId;

    @Schema(description = "版本")
    private String version;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "类型")
    private DynamicFormFieldType type;

    @Schema(description = "必填")
    private String required;

    @Schema(description = "默认值")
    private String defaultValue;

    @Schema(description = "提示")
    private String placeholder;

//    @Schema(description = "单选多选配置")
//    private String options;

    @Schema(description = "单选多选配置")
    @TableField(typeHandler = Fastjson2TypeHandler.class)
    private List<DynamicFormOption> options = new ArrayList<>();

    @Schema(description = "最小长度")
    private Integer minLength;

    @Schema(description = "最大长度")
    private Integer maxLength;

    @Schema(description = "最小值")
    private Integer min;

    @Schema(description = "最大值")
    private Integer max;

    @Schema(description = "正则表达式")
    private String pattern;

    @Schema(description = "正则表达式对应提示")
    private String patternTips;

    @Schema(description = "宽度  1 - 24")
    private Integer span = 24;

    @Schema(description = "逻辑删除")
    @TableLogic
    private String deleted = "0";

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime updateTime;




    private static boolean getDynamicFormField(DynamicFormField dynamicFormField){

        if(!StringUtils.hasText(dynamicFormField.getTitle())){
            return false;
        }
        if(Objects.nonNull(dynamicFormField.getMinLength())
                && Objects.nonNull(dynamicFormField.getMaxLength())
                && dynamicFormField.getMinLength() > dynamicFormField.getMaxLength()){
            return false;
        }
        if(Objects.nonNull(dynamicFormField.getMin())
                && Objects.nonNull(dynamicFormField.getMax())
                && dynamicFormField.getMin() > dynamicFormField.getMax()){
            return false;
        }


        return true;
    }







}