package com.cat.common.entity.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/***
 * <TODO description class purpose>
 * @title LoginInfo
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/24 23:02
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "LoginInfo", description = "登录请求信息")
public class LoginInfo {

    /**
     * 用户名/账号
     */
    @Schema(description = "用户名/账号")
    private String username;

    /**
     * 密码
     */
    @Schema(description = "密码")
    private String password;

    /**
     * 令牌
     */
    @Schema(description = "令牌")
    private String token;

}
