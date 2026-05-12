package com.cat.common.entity.process;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;
import java.io.Serial;
import java.util.Map;

/**
 * <p>
 * 
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-03-01
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName(value = "cat_process_definition_bytearray", autoResultMap = true)
@Schema(name = "ProcessDefinitionBytearray", description = "")
public class ProcessDefinitionBytearray implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "流程定义id")
    private Integer id;

    @Schema(description = "xml 字节流")
    private byte[] xml;

    @Schema(description = "logicFlow data")
    @TableField(value = "raw_data",typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> rawData;

}