package com.cat.common.entity.process;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_process_gateway_condition")
@Schema(name = "ProcessGatewayCondition", description = "流程网关条件配置")
public class ProcessGatewayCondition implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "流程定义ID")
    private Integer processDefinitionId;

    @Schema(description = "版本: DRAFT / 1 / 2")
    private String version;

    @Schema(description = "BPMN sequenceFlow ID")
    private String sequenceFlowId;

    @Schema(description = "源节点ID")
    private String sourceNodeId;

    @Schema(description = "目标节点ID")
    private String targetNodeId;

    @Schema(description = "条件类型: NATIVE / CUSTOM")
    private String conditionType;

    @Schema(description = "是否默认走向: 0-否 1-是")
    private Boolean isDefault;

    @Schema(description = "NATIVE模式时的JUEL表达式")
    private String nativeExpression;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @TableField(exist = false)
    @Schema(description = "CUSTOM模式时的规则树")
    private List<ProcessGatewayConditionNode> ruleTree;
}
