package com.cat.common.entity.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 流程任务操作参数（认领 / 通过 / 拒绝等通用）
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-04-13
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "ProcessHandleParam", description = "流程任务操作参数")
public class ProcessHandleParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "自建流程实例id")
    private Integer processInstanceId;

    @Schema(description = "Flowable任务id")
    private String taskId;

    @Schema(description = "备注/审批意见")
    private String remark;

    @Schema(description = "目标节点id")
    private String targetNodeId;

    @Schema(description = "目标节点名称")
    private String targetNodeName;

    @Schema(description = "流程定义id（发起/保存草稿时必填）")
    private Integer processDefinitionId;

    @Schema(description = "流程标题（发起/保存草稿时使用）")
    private String title;

    @Schema(description = "节点表单数据")
    private Map<String, Object> nodeFormData;

    @Schema(description = "全局表单数据")
    private Map<String, Object> globalFormData;

    @Schema(description = "需选择的处理人节点的候选人已选择人员")
    private Map<String, List<Integer>> nodeCandidateUsersChoose;



}