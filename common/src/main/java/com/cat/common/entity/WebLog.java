package com.cat.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;
import java.io.Serial;
/**
 * <p>
 * web日志表
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-01-06
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_web_log")
@Schema(name = "WebLog", description = "web日志表")
public class WebLog implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "请求地址")
    private String reqPath;

    @Schema(description = "请求方法")
    private String reqMethod;

    @Schema(description = "请求参数")
    private String reqArgs;

    @Schema(description = "类名")
    private String className;

    @Schema(description = "响应")
    private String repArgs;

    @Schema(description = "方法名")
    private String methodName;

    @Schema(description = "客户端地址")
    private String remoteAddr;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "响应时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime endTime;

    @Schema(description = "响应耗时 单位: 毫秒")
    private long resTime;


    @Schema(description = "用户id")
    private String userId;


    @TableField(exist = false)
    @Schema(description = "请求开始时间戳")
    private long startTimestamp;

    @TableField(exist = false)
    @Schema(description = "请求结束时间戳")
    private long endTimestamp;




}