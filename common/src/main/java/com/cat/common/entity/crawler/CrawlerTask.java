package com.cat.common.entity.crawler;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
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
 * @since 2025-05-08
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_crawler_task")
@Schema(name = "CrawlerTask", description = "")
public class CrawlerTask implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务id")
    @TableId(type = IdType.AUTO)
    private Integer id;

    @Schema(description = "任务名称")
    private String name;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "脚本文件id")
    private String fileId;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}