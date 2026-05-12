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
 * 动态表单字段联动条件节点
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-05-06
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName(value = "cat_dynamic_form_linkage_node", autoResultMap = true)
@Schema(name = "DynamicFormLinkageNode", description = "动态表单字段联动条件节点")
public class DynamicFormLinkageNode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "节点id")
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @Schema(description = "所属规则ID")
    private String ruleId;

    @Schema(description = "表单id")
    private String formId;

    @Schema(description = "版本")
    private String version;

    @Schema(description = "父节点ID，null表示根节点")
    private String parentId;

    @Schema(description = "节点类型：AND(与)/OR(或)/CONDITION(条件)")
    private String nodeType;

    @Schema(description = "触发字段 fieldId，仅CONDITION有效")
    private String triggerFieldId;

    @Schema(description = "触发条件：EQ/NE/GT/LT/GE/LE/IN/NOT_IN/EMPTY/NOT_EMPTY/REGEX，仅CONDITION有效")
    private String triggerCondition;

    @Schema(description = "触发值，仅CONDITION有效")
    @TableField(typeHandler = JsonValueTypeHandler.class)
    private Object triggerValue;

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

    @Schema(description = "子节点（仅前端交互/内存组装使用，不持久化到本表）")
    @TableField(exist = false)
    private List<DynamicFormLinkageNode> children;

}
