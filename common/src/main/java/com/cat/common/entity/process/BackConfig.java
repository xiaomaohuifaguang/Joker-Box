package com.cat.common.entity.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Schema(name = "BackConfig", description = "节点驳回配置")
public class BackConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "是否允许驳回")
    private boolean allowBack;

    @Schema(description = "驳回方式: prev/specific/choose")
    private String backType;

    @Schema(description = "固定驳回目标节点id")
    private String backNodeId;

    @Schema(description = "回退后分配策略: auto/last_handler/reassign")
    private String backAssigneePolicy;

    @Schema(description = "该节点允许的操作按钮")
    private List<String> actionButtons;
}
