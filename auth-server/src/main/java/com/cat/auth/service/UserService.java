package com.cat.auth.service;

import com.cat.common.entity.*;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;

import java.io.IOException;
import java.util.List;

/***
 * 鉴权服务业务层接口
 * @title AuthService
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/24 23:00
 **/
public interface UserService {

    /**
     * 获取token
     * @param loginInfo 登录信息
     * @return token
     */
    String getToken(LoginInfo loginInfo);

    /**
     * 通过用户名获取登录用户信息
     * @param username 用户名
     * @return 登录用户信息
     */
    LoginUser getLoginUser(String username);

    /**
     * 通过令牌获取登录用户信息
     * @param token 令牌
     * @return 登录用户信息
     */
    LoginUser getLoginUserByToken(String token);

    /**
     * 通过用户名获取用户
     * @param username 用户名
     * @return 用户
     */
    User getUserByUsername(String username);

    /**
     * 通过userid获取用户角色
     * @param userId 用户id
     * @return 用户角色
     */
    List<Role> getRoleByUserId(String userId);


    /**
     * 获取登录信息
     * @return 用户登录信息
     */
    UserInfo getUserInfo();


    /**
     * 发送验证码
     * @param mail 邮箱地址
     */
    void code(String mail) throws TemplateException, MessagingException, IOException;

    DTO<?> register(RegisterUserInfo registerUserInfo);

    Page<User> queryPage(UserPageParam pageParam);

    DTO<?> delete(String userId);

    User getUserInfo(String userId);

    DTO<?> addRole(String userId, String roleId);

    DTO<?> deleteRole(String userId, String roleId);

    DTO<?> resetPassword(String userId);

}
