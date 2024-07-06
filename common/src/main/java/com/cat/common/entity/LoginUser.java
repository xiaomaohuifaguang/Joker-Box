package com.cat.common.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/***
 * 登录用户信息
 * @title LoginUser
 * @description 登录用户信息
 * @author xiaomaohuifaguang
 * @create 2024/6/20 22:24
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class LoginUser {

    /**
     *  用户id
     */
    private String userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 密码
     */
    private String password;

    /**
     * 用户类型
     */
    private String type;

    /**
     * 角色
     */
    private List<Role> roles;


    public LoginUser(User user, List<Role> roles) {
        setUserId(user.getId());
        setUsername(user.getUsername());
        setPassword(user.getPassword());
        setNickname(user.getNickname());
        setType(user.getType());
        setRoles(roles);
    }
}
