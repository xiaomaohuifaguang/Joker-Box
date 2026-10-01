package com.cat.common.entity.process;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "ProcessTrack", description = "流程追踪")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessTrack implements Serializable {


    @Serial
    private static final long serialVersionUID = 1L;


    @Schema(description = "已走过的节点id")
    private Set<String> doneNodeIds = new HashSet<>();

    @Schema(description = "走过的连线id(=BPMN sequenceFlow id)")
    private Set<String> passedEdgeIds = new HashSet<>();

    @Schema(description = "当前所有活动节点id")
    private Set<String> activeNodeIds = new HashSet<>();

    @Schema(description = "当前任务节点id")
    private String currentNodeId;


}
