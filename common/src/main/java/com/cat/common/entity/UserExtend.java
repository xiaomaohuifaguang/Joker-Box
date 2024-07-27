package com.cat.common.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;
import java.io.Serial;
/**
 * <p>
 * 用户拓展表
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2024-07-09
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_user_extend")
@Schema(name = "UserExtend", description = "用户拓展表")
public class UserExtend implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "扩展表")
    @TableId("user_id")
    private Integer userId;

    @Schema(description = "性别")
    private String sex;

    @Schema(description = "邮箱")
    private String mail;

    @Schema(description = "手机号")
    private Long phone;
}