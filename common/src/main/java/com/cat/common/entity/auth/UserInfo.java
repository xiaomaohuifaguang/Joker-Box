package com.cat.common.entity.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;


/***
 * <TODO description class purpose>
 * @title UserInfo
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/6 23:16
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "UserInfo", description = "用户登录信息")
public class UserInfo {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户昵称")
    private String nickname;



}
