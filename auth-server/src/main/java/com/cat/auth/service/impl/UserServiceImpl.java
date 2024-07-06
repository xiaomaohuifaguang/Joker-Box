package com.cat.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cat.auth.config.redis.RedisService;
import com.cat.auth.mapper.RoleMapper;
import com.cat.auth.mapper.UserMapper;
import com.cat.auth.service.UserService;
import com.cat.common.entity.*;
import com.cat.common.utils.CryptoUtils;
import com.cat.common.utils.JwtUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

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
    private RoleMapper roleMapper;
    @Resource
    private RedisService redisService;

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
        List<Role> roles = roleMapper.getRolesByUserId(user.getId()); // 获取角色通过userId
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
