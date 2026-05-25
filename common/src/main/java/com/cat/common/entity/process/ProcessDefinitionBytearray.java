package com.cat.common.entity.process;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName(value = "cat_process_definition_bytearray", autoResultMap = true)
@Schema(name = "ProcessDefinitionBytearray", description = "流程定义BPMN数据（按版本隔离）")
public class ProcessDefinitionBytearray implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "流程定义ID（逻辑关联，无外键）")
    private Integer processDefinitionId;

    @Schema(description = "版本：DRAFT / 1 / 2 / 3 ...")
    private String version;

    @Schema(description = "xml 字节流")
    private byte[] xml;

    @Schema(description = "logicFlow data")
    @TableField(value = "raw_data", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> rawData;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
