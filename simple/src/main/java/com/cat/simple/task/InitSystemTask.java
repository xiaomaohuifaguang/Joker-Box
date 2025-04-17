package com.cat.simple.task;

import com.alibaba.fastjson2.JSONObject;
import com.cat.common.entity.auth.Role;
import com.cat.common.entity.auth.User;
import com.cat.common.utils.CryptoUtils;
import com.cat.simple.mapper.RoleMapper;
import com.cat.simple.mapper.UserMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.cat.common.entity.CONSTANTS.*;

@Slf4j
@Component
public class InitSystemTask {


    @Resource
    private UserMapper userMapper;

    @Resource
    private RoleMapper roleMapper;


    private static final int ADMIN_ID = 1;


    /**
     * 初始化引导 系统缓存数据
     */
    @PostConstruct
    private void initBoot(){

    }


    /**
     * 人员权限初始化
     */
    @PostConstruct
    private void initAuth(){

        // 初始化 系统 查看是否拥有系统运行必要 用户及角色
        log.info("验证系统必备信息");


        User user = userMapper.selectById(ADMIN_ID);
        if(ObjectUtils.isEmpty(user)){
            log.info("超级管理员不存在!!!");
            log.info("初始化超级管理员...");
            user = new User()
                    .setId(ADMIN_ID)
                    .setType(USER_TYPE_USER)
                    .setUsername("admin")
                    .setPassword(CryptoUtils.encrypt(DEFAULT_PASSWORD))
                    .setNickname("超级管理员")
                    .setCreateTime(LocalDateTime.now());
            int insert = userMapper.insert(user);
            log.info("初始化超级管理员完成");
        }else {
            log.info("超级管理员已经存在");
        }


        Role adminRole = roleMapper.selectById(ROLE_ADMIN_CODE);

        if(ObjectUtils.isEmpty(adminRole)){
            log.info("超级管理员角色不存在!!!");
            log.info("初始化超级管理员角色...");
            adminRole = new Role().setId(ROLE_ADMIN_CODE).setName("超级管理员").setCreateTime(LocalDateTime.now());
            int insert = roleMapper.insert(adminRole);
            log.info("初始化超级管理员角色完成");
        }else {
            log.info("超级管理员角色已经存在");
        }


        Role everyOneRole = roleMapper.selectById(ROLE_EVERYONE_CODE);
        if(ObjectUtils.isEmpty(everyOneRole)){
            log.info("通用用户角色不存在!!!");
            log.info("初始化通用用户角色...");
            everyOneRole = new Role().setId(ROLE_EVERYONE_CODE).setName("普通用户").setCreateTime(LocalDateTime.now());
            int insert = roleMapper.insert(everyOneRole);
            log.info("初始化通用用户角色完成");
        }else {
            log.info("通用用户角色已经存在");
        }


        List<Role> rolesByUserId = roleMapper.getRolesByUserId(String.valueOf(ADMIN_ID));
        Set<Integer> collect = rolesByUserId.stream().map(Role::getId).collect(Collectors.toSet());
        if(CollectionUtils.isEmpty(collect) || !collect.contains(ROLE_ADMIN_CODE) || !collect.contains(ROLE_EVERYONE_CODE)){
            log.info("用户：超级管理员角色不完整!!!");
            collect = CollectionUtils.isEmpty(collect) ? new HashSet<>() : collect;
            if(!collect.contains(ROLE_ADMIN_CODE)){
                userMapper.insertUserAndRole(String.valueOf(ADMIN_ID),String.valueOf(ROLE_ADMIN_CODE), LocalDateTime.now());
            }

            if(!collect.contains(ROLE_EVERYONE_CODE)){
                userMapper.insertUserAndRole(String.valueOf(ADMIN_ID),String.valueOf(ROLE_EVERYONE_CODE), LocalDateTime.now());

            }

        }


    }



}
