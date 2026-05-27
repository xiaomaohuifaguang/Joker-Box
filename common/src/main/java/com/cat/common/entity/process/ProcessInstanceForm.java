package com.cat.common.entity.process;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_process_instance_form")
@Schema(name = "ProcessInstanceForm", description = "流程实例表单关联")
public class ProcessInstanceForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "流程实例ID")
    private Integer processInstanceId;

    @Schema(description = "节点ID，null=主表单")
    private String nodeId;

    @Schema(description = "表单ID")
    private String formId;

    @Schema(description = "表单版本（启动时快照）")
    private String formVersion;

    @Schema(description = "表单实例ID")
    private String formInstanceId;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}