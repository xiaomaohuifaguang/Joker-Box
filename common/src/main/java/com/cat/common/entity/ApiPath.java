package com.cat.common.entity;

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
 * 
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2024-07-11
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_api_path")
@Schema(name = "ApiPath", description = "")
public class ApiPath implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "api路径")
    private String path;

    @Schema(description = "服务名称")
    private String server;

    @Schema(description = "是否白名单 1是0否")
    private String whiteList;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "分组名称")
    private String groupName;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime createTime = LocalDateTime.now();

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime updateTime = LocalDateTime.now();

    @Schema(description = "角色是否绑定")
    @TableField(exist = false)
    private boolean roleBind;

    public String getWhiteListStr() {
        return whiteList.equals("1") ? "是":"否";
    }
}