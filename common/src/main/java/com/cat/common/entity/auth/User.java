package com.cat.common.entity.auth;

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
 * 用户表
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2024-06-23
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_user")
@Schema(name = "User", description = "用户表")
public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "用户类型 0 用户 1系统")
    private String type = "0";

    @Schema(description = "删除标识 0 否 1是")
    @TableLogic // 逻辑删除
    private String deleted = "0";

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime createTime = LocalDateTime.now();

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime updateTime;

    public String getIdStr(){
        return String.format("%010d", this.id);
    }

    public Integer getId(){
        return this.id;
    }

    @Schema(description = "用户扩展信息")
    @TableField(exist = false)
    private UserExtend userExtend;

}