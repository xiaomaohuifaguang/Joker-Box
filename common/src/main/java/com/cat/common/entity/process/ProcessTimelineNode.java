package com.cat.common.entity.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程节点时间线，用于直观展示流程处理轨迹。
 */
@Data
@Schema(name = "ProcessTimelineNode", description = "流程节点时间线")
public class ProcessTimelineNode {

    @Schema(description = "BPMN节点ID")
    private String nodeId;

    @Schema(description = "节点名称")
    private String nodeName;

    @Schema(description = "审批轮次（驳回后重新经过此节点时+1）")
    private Integer round;

    @Schema(description = "节点状态: completed-已完成, active-进行中/待办")
    private String nodeStatus;

    @Schema(description = "节点开始时间（该轮次最早处理时间）")
    private LocalDateTime startTime;

    @Schema(description = "节点完成时间（该轮次最晚处理时间）")
    private LocalDateTime endTime;

    @Schema(description = "该节点此轮次的所有处理记录（会签时有多条）")
    private List<ProcessHandleInfo> handlers;
}