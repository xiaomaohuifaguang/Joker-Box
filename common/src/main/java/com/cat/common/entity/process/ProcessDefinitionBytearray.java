package com.cat.common.entity.process;

import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;
import java.io.Serial;
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
@TableName("cat_process_definition_bytearray")
@Schema(name = "ProcessDefinitionBytearray", description = "")
public class ProcessDefinitionBytearray implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "流程定义id")
    private Integer id;

    @Schema(description = "xml 字节流")
    private byte[] xml;
}