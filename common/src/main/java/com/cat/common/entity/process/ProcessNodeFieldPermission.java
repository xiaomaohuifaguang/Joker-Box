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
@TableName("cat_process_node_field_permission")
@Schema(name = "ProcessNodeFieldPermission", description = "节点字段权限")
public class ProcessNodeFieldPermission implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "流程定义ID（逻辑关联，无外键）")
    private Integer processDefinitionId;

    @Schema(description = "版本：DRAFT / 1 / 2 / 3 ...")
    private String version;

    @Schema(description = "BPMN节点ID")
    private String nodeId;

    @Schema(description = "字段标识（对应表单 field.key）")
    private String fieldKey;

    @Schema(description = "权限：VISIBLE / READONLY / HIDDEN / EDITABLE / REQUIRED")
    private String permission;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
