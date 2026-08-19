package com.cat.common.entity.process;

import com.cat.common.entity.auth.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashSet;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "NextUserTaskInfo", description = "下一个用户任务")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NextUserTaskInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "审批类型")
    private int type;

    @Schema(description = "节点id")
    private String nodeId;

    @Schema(description = "节点名称")
    private String nodeName;

    @Schema(description = "候选人")
    private LinkedHashSet<User> candidateUsers;

    public NextUserTaskInfo(int type, String nodeId, String nodeName) {
        this.type = type;
        this.nodeId = nodeId;
        this.nodeName = nodeName;
    }
}
