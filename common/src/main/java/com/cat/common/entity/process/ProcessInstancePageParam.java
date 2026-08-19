package com.cat.common.entity.process;

import com.cat.common.entity.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "ProcessInstancePageParam", description = "流程实例分页查询参数")
@EqualsAndHashCode(callSuper = false)
public class ProcessInstancePageParam extends PageParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "查询类型 0 草稿 1 我发起的(进行中) 2 审批待办 3 审批待认领 4 审批已办 5 我发起的(全部) 6 申请人待办")
    private String type;

    @Schema(description = "用户id（服务端自动填充）")
    private String userId;

    @Schema(description = "流程定义id")
    private Integer processDefinitionId;

    @Schema(description = "流程状态")
    private String processStatus;

    @Schema(description = "创建时间起始")
    private String startTime;

    @Schema(description = "创建时间结束")
    private String endTime;

    public void init() {
        type = Objects.isNull(type) ? "" : type;
        switch (type) {
            case "0", "1", "2", "3", "4", "5", "6":
                break;
            default:
                setType("-1");
        }
    }
}