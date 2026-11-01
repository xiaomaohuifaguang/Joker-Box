package com.cat.common.entity.process;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.util.CollectionUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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


    @Schema(description = "流程处理记录")
    private List<ProcessHandleInfo> processHandleInfoList;

    @Schema(description = "节点轮次处理记录")
    private Map<String, List<RoundRecordVO>> nodeRoundRecordInfoList;


    public void setProcessHandleInfoList(List<ProcessHandleInfo> processHandleInfoList) {
        this.processHandleInfoList = processHandleInfoList;
        this.nodeRoundRecordInfoList = new LinkedHashMap<>();

        if (CollectionUtils.isEmpty(processHandleInfoList)) {
            return;
        }

        Map<String, List<ProcessHandleInfo>> byNode = processHandleInfoList.stream()
                .collect(Collectors.groupingBy(
                        ProcessHandleInfo::getTaskDefinitionKey,
                        LinkedHashMap::new,
                        Collectors.toList()));

        byNode.forEach((nodeId, nodeRecords) -> {
            Map<Integer, List<ProcessHandleInfo>> byRound = nodeRecords.stream()
                    .collect(Collectors.groupingBy(
                            ProcessHandleInfo::getRound,
                            TreeMap::new,               // round 自动升序
                            Collectors.toList()));

            List<RoundRecordVO> roundInfos = byRound.entrySet().stream()
                    .map(e -> new RoundRecordVO(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());

            this.nodeRoundRecordInfoList.put(nodeId, roundInfos);
        });
    }

}
