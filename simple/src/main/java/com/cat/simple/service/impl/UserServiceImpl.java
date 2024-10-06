package com.cat.simple.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.simple.config.redis.RedisService;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.mapper.RoleMapper;
import com.cat.simple.mapper.UserExtendMapper;
import com.cat.simple.mapper.UserMapper;
import com.cat.simple.service.MailService;
import com.cat.simple.service.UserService;
import com.cat.common.entity.CONSTANTS;
import com.cat.common.entity.DTO;
import com.cat.common.entity.Page;
import com.cat.common.entity.auth.*;
import com.cat.common.utils.CryptoUtils;
import com.cat.common.utils.JwtUtils;
import com.cat.common.utils.RegexUtils;
import freemarker.template.TemplateException;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/***
 * 鉴权服务业务层实现
 * @title AuthServiceImpl
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/24 23:01
 **/
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final long tokenExpire = 14 * 24 * 60 * 60;

    @Resource
    private UserMapper userMapper;
    @Resource
    private UserExtendMapper userExtendMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private RedisService redisService;
    @Resource
    private MailService mailService;

    @Override
    public String getToken(LoginInfo loginInfo) {
        LoginUser loginUser = this.getLoginUser(loginInfo.getUsername());
        return CryptoUtils.verify(loginInfo.getPassword(), loginUser.getPassword()) ? this.makeToken(loginUser) : null;
    }

    @Override
    public LoginUser getLoginUser(String username) {
        // 缓存读取
        LoginUser loginUser = redisService.get(CONSTANTS.REDIS_PARENT_TOKEN + username, LoginUser.class);
        if (!ObjectUtils.isEmpty(loginUser)) return loginUser;

        // 数据库读取
        User user = this.getUserByUsername(username); // 获取用户通过username
        List<Role> roles = roleMapper.getRolesByUserId(user.getIdStr()); // 获取角色通过userId
        loginUser = new LoginUser(user, roles);
        redisService.set(CONSTANTS.REDIS_PARENT_TOKEN + loginUser.getUsername(), loginUser, tokenExpire); // 存储缓存redis
        return loginUser;
    }

    @Override
    public LoginUser getLoginUserByToken(String token) {
        token = token.replace(CONSTANTS.TOKEN_TYPE + " ", "");
        Map<String, Object> decrypt = JwtUtils.decrypt(token);
        if (ObjectUtils.isEmpty(decrypt)) return null;
        String userId = (String) decrypt.get("userId");
        String username = (String) decrypt.get("username");
        String password = (String) decrypt.get("password");
        LoginUser loginUser = this.getLoginUser(username);
        return userId.equals(loginUser.getUserId()) && password.equals(loginUser.getPassword()) ? loginUser : null;
    }

    @Override
    public User getUserByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", "0");
        queryWrapper.eq("username", username);
        return userMapper.selectOne(queryWrapper);
    }

    @Override
    public List<Role> getRoleByUserId(String userId) {
        return roleMapper.getRolesByUserId(userId);
    }

    @Override
    public UserInfo getUserInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        UserInfo userInfo = new UserInfo();
        BeanUtils.copyProperties(loginUser, userInfo);
        return userInfo;
    }

    @Override
    public void code(String mail) throws TemplateException, MessagingException, IOException {
        mailService.sendCode(mail);
    }

    @Override
    @Transactional
    public DTO<?> register(RegisterUserInfo registerUserInfo) {
        if(!RegexUtils.validate(registerUserInfo.getUsername(), RegexUtils.ACCOUNT_REGEX)
                || !RegexUtils.validate(registerUserInfo.getPassword(), RegexUtils.PASSWORD_REGEX)
                || !RegexUtils.validate(registerUserInfo.getMail(), RegexUtils.EMAIL_REGEX) ){
            return DTO.error("格式错误");
        }

        User userByUsername = getUserByUsername(registerUserInfo.getUsername());
        if(!ObjectUtils.isEmpty(userByUsername)){
            return DTO.error("用户名已存在");
        }

        String codeCache = redisService.get(CONSTANTS.REDIS_PARENT_MAIL_CODE + registerUserInfo.getMail(), String.class);
        if(!StringUtils.hasText(codeCache) || !codeCache.equals(registerUserInfo.getCode())){
            return DTO.error("验证码不正确");
        }

        User user = new User();
        BeanUtils.copyProperties(registerUserInfo,user);
        user.setPassword(CryptoUtils.encrypt(user.getPassword()));
        userMapper.insert(user);

        userByUsername = getUserByUsername(registerUserInfo.getUsername());
        UserExtend userExtend = new UserExtend().setUserId(userByUsername.getId());
        BeanUtils.copyProperties(registerUserInfo,userExtend);
        userExtend.setSex(StringUtils.hasText(userExtend.getSex()) && (userExtend.getSex().equals("男")||userExtend.getSex().equals("女")) ? userExtend.getSex() : "未知");
        userExtendMapper.insert(userExtend);
        roleMapper.defaultRole(userByUsername.getId());
        return DTO.success();
    }

    @Override
    public Page<User> queryPage(UserPageParam pageParam) {
        Page<User> page = new Page<>(pageParam);
        page = userMapper.selectPage(page,pageParam);
        page.getRecords().forEach(u->{
            UserExtend userExtend = userExtendMapper.selectById(u.getId());
            u.setUserExtend(userExtend);
        });
        return page;
    }

    @Override
    public DTO<?> delete(String userId) {
        if(!userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getId,userId))){
            return DTO.error("用户不存在");
        }

        List<Role> roles = roleMapper.getRolesByUserId(userId);
        List<Integer> roleIds = roles.stream().map(Role::getId).toList();
        if(roleIds.contains(CONSTANTS.ROLE_ADMIN_CODE)){
            return DTO.error("大胆,该用户为管理员");
        }
        userMapper.deleteById(userId);
        return DTO.success();
    }

    @Override
    public User getUserInfo(String userId) {
        User user = userMapper.selectById(userId);
        if(!ObjectUtils.isEmpty(user)){
            UserExtend userExtend = userExtendMapper.selectById(userId);
            user.setUserExtend(ObjectUtils.isEmpty(userExtend) ? new UserExtend() : userExtend);
        }
        return user;
    }

    @Override
    @Transactional
    public DTO<?> addRole(String userId, String roleId) {

        if(userMapper.userCountByRole(String.valueOf(CONSTANTS.ROLE_ADMIN_CODE)) > 0){
            return DTO.error("超级管理员仅限一个用户");
        }

        if(!userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getId,userId))){
            return DTO.error("用户不存在");
        }

        if(!roleMapper.exists(new LambdaQueryWrapper<Role>().eq(Role::getId,roleId))){
            return DTO.error("角色不存在");
        }

        List<Integer> list = getRoleByUserId(userId).stream().map(Role::getId).toList();
        if(list.contains(Integer.parseInt(roleId))){
            return DTO.error("角色已绑定,无需重复绑定");
        }

        userMapper.insertUserAndRole(userId,roleId, LocalDateTime.now());

        return DTO.success();
    }

    @Override
    public DTO<?> deleteRole(String userId, String roleId) {
        if(roleId.equals(String.valueOf(CONSTANTS.ROLE_ADMIN_CODE)) &&  userId.equals("1")){
            return DTO.error("大傻春，你要干什么");
        }
        if(roleId.equals(String.valueOf(CONSTANTS.ROLE_EVERYONE_CODE))){
            return DTO.error("默认角色不建议修改");
        }
        int del = userMapper.removeUserAndRole(userId, roleId);
        return DTO.success();
    }

    @Override
    @Transactional
    public DTO<?> resetPassword(String userId) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getId, userId));
        if(ObjectUtils.isEmpty(user)){
            return DTO.error("用户不存在");
        }
        userMapper.update(new LambdaUpdateWrapper<User>().set(User::getPassword,CryptoUtils.encrypt("12345678")).eq(User::getId,userId));

        return DTO.success();
    }

    private String makeToken(LoginUser loginUser) {
        if(loginUser.getType().equals(CONSTANTS.USER_TYPE_SERVER)){
            return JwtUtils.encrypt(new HashMap<>() {{
                put("userId", loginUser.getUserId());
                put("username", loginUser.getUsername());
                put("password", loginUser.getPassword());
            }}, 0);
        }


        return JwtUtils.encrypt(new HashMap<>() {{
            // 随机插入 随机字符串
            for (int start = 0; start < new Random().nextInt(2); start++) {
                put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
            }
            put("userId", loginUser.getUserId());
            // 随机插入 随机字符串
            for (int start = 0; start < new Random().nextInt(2); start++) {
                put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
            }
            put("username", loginUser.getUsername());
            // 随机插入 随机字符串
            for (int start = 0; start < new Random().nextInt(2); start++) {
                put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
            }
            // 随机插入 随机字符串
            put("password", loginUser.getPassword());
            // 随机插入 随机字符串
            for (int start = 0; start < new Random().nextInt(2); start++) {
                put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
            }
        }}, tokenExpire);
    }


}
