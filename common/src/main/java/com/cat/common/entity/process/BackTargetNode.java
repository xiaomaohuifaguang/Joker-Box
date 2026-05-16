package com.cat.common.entity.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@Accessors(chain = true)
@Schema(name = "BackTargetNode", description = "可驳回目标节点")
public class BackTargetNode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "BPMN节点id")
    private String nodeId;

    @Schema(description = "节点名称")
    private String nodeName;
}
