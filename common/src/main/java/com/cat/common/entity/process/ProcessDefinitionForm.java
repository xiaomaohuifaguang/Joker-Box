package com.cat.common.entity.process;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("cat_process_definition_form")
@Schema(name = "ProcessDefinitionForm", description = "流程定义-表单绑定")
public class ProcessDefinitionForm implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "流程定义ID（逻辑关联，无外键）")
    private Integer processDefinitionId;

    @Schema(description = "版本：DRAFT / 1 / 2 / 3 ...")
    private String version;

    @Schema(description = "表单ID")
    private String formId;

    @Schema(description = "绑定的表单版本号（快照锁定）")
    private String formVersion;

    @Schema(description = "绑定类型：GLOBAL-全局默认 / NODE-节点绑定")
    private String bindType;

    @Schema(description = "BPMN节点ID，GLOBAL时为空")
    private String nodeId;

    @Schema(description = "节点是否继承主表单字段：0-否 / 1-是")
    private String inheritMainForm;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
