package com.cat.common.entity.process;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;
import java.io.Serial;
/**
 * <p>
 * 流程定义信息表
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-02-27
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_process_definition")
@Schema(name = "ProcessDefinition", description = "流程定义信息表")
public class ProcessDefinition implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "流程id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "流程定义key	ACT_RE_PROCDEF")
    private String processKey;

    @Schema(description = "流程定义名称")
    private String processName;

    @Schema(description = "流程描述")
    private String processDescription;

    @Schema(description = "使用版本")
    private String version;

    @Schema(description = "流程状态，比如 0草稿、1已发布、-1已停用")
    private String status;

    @Schema(description = "创建人userid")
    private String createBy;

    @Schema(description = "创建人昵称")
    @TableField(exist = false)
    private String createByName;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime updateTime;

    @Schema(description = "逻辑删除")
    @TableLogic
    private String deleted;

    @Schema(description = "流程配置文件 bpmn")
    @TableField(exist = false)
    private String xmlStr;

}