package com.cat.common.entity;

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
 * 文件信息
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2024-06-26
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_file_info")
@Schema(name = "FileInfo", description = "文件信息")
public class FileInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "文件唯一id")
    private String id;

    @Schema(description = "文件名")
    private String filename;

    @Schema(description = "文件类型")
    private String contentType;

    @Schema(description = "类型 file 文件 folder 文件夹")
    private String type;

    @Schema(description = "父级文件夹id 根目录0")
    private String parentId;

    @Schema(description = "文件大小")
    private long size;

    @Schema(description = "创建人id")
    private Integer userId;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime createTime = LocalDateTime.now();

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime updateTime;

}