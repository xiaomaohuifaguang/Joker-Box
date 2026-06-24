package com.cat.common.entity.process;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_process_gateway_condition_node")
@Schema(name = "ProcessGatewayConditionNode", description = "网关条件规则树节点")
public class ProcessGatewayConditionNode implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "关联条件配置ID")
    private Long conditionId;

    @Schema(description = "父节点ID，null=根节点")
    private Long parentId;

    @Schema(description = "节点类型: AND / OR / CONDITION")
    private String nodeType;

    @Schema(description = "条件来源分类: FORM_FIELD / HANDLER_DEPT / HANDLER_ROLE / PREV_HANDLER_DEPT / PREV_HANDLER_ROLE")
    private String category;

    @Schema(description = "字段标识(fieldId或内置变量名)")
    private String fieldKey;

    @Schema(description = "运算符: EQ / NE / GT / LT / GE / LE / IN / NOT_IN / EMPTY / NOT_EMPTY / REGEX")
    private String operator;

    @Schema(description = "比较值")
    private String value;

    @Schema(description = "同级排序")
    private Integer sort;

    @TableField(exist = false)
    @Schema(description = "子节点")
    private List<ProcessGatewayConditionNode> children;
}
