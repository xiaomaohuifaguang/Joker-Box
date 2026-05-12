package com.cat.common.entity.dynamicForm;

import com.baomidou.mybatisplus.annotation.*;
import com.cat.common.handler.JsonValueTypeHandler;
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
 * 动态表单字段联动规则
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-05-06
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName(value = "cat_dynamic_form_linkage_rule", autoResultMap = true)
@Schema(name = "DynamicFormLinkageRule", description = "动态表单字段联动规则")
public class DynamicFormLinkageRule implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "规则id")
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @Schema(description = "表单id")
    private String formId;

    @Schema(description = "版本")
    private String version;

    @Schema(description = "规则名称")
    private String name;

    @Schema(description = "目标字段 fieldId")
    private String targetFieldId;

    @Schema(description = "动作类型：SHOW(显示)/HIDE(隐藏)/REQUIRED(必填)/OPTION(设置选项)/VALUE(设置值)/DISABLED(禁用)/ENABLED(启用)/SET_PATTERN(设置正则)/SET_SPAN(设置宽度)")
    private String actionType;

    @Schema(description = "动作参数")
    @TableField(typeHandler = JsonValueTypeHandler.class)
    private Object actionValue;

    @Schema(description = "是否启用：1启用 0禁用")
    private Boolean enable;

    @Schema(description = "排序")
    private Integer sortOrder;

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

    @Schema(description = "条件节点树（仅前端交互使用，不持久化到本表）")
    @TableField(exist = false)
    private List<DynamicFormLinkageNode> conditionTree;

}
