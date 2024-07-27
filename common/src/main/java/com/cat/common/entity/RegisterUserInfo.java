package com.cat.common.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/***
 * <TODO description class purpose>
 * @title RegisterUserInfo
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/9 23:26
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "RegisterUserInfo", description = "注册信息")
public class RegisterUserInfo {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String mail;

    @Schema(description = "验证码")
    private String code;

    @Schema(description = "邀请码")
    private String inviteCode;

    @Schema(description = "性别")
    private String sex;

    @Schema(description = "手机号")
    private Long phone;




}
