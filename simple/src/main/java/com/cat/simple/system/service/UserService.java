package com.cat.simple.system.service;

import com.cat.common.entity.DTO;
import com.cat.common.entity.Page;
import com.cat.common.entity.auth.*;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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
     * 获取token令牌剩余时间
     * @param token 令牌
     * @return 剩余时间ms
     */
    long getTokenExpirationTimeLeftMillis(String token);

    /**
     * 通过用户名获取登录用户信息
     * @param username 用户名
     * @return 登录用户信息
     */
    LoginUser getLoginUser(String username);

    LoginUser getLoginUser(String clientName, Integer clientId);

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
     * 通过客户端信息获取用户
     * @param clientName 客户端名称
     * @param clientId 客户端ID
     * @return 用户
     */
    User getUserByClient(String clientName, Integer clientId);

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

    DTO<?> register(RegisterUserInfo registerUserInfo, boolean verifyMainAndCode);

    Page<User> queryPage(UserPageParam pageParam);

    DTO<?> delete(String userId);

    User getUserInfo(String userId);

    DTO<?> addRole(String userId, String roleId);

    DTO<?> deleteRole(String userId, String roleId);

    DTO<?> resetPassword(String userId);

    boolean avatarUpload(MultipartFile file) throws IOException;

    void avatarUpload(String url, String username) throws IOException;

    void avatar(String userId) throws IOException;

    DTO<?> changePassword(String oldPassword, String newPassword) throws IOException;


    void clearUserCache();


    String makeToken(LoginUser loginUser);

    String getTokenBySSO(String clientName, String id);

    boolean exist(String username);


    Map<String, String> generateQRCodeImage() throws Exception;

    DTO<?> updateUserInfo(UserInfo userInfo);


    List<Org> getOrgByUserId(String userId);

    DTO<?> addOrg(String userId, String orgId);

    DTO<?> deleteOrg(String userId, String orgId);

    List<User> selectorUserWithInfo(String search);

    List<User> selectorInitByIds(List<Integer> ids);



}
