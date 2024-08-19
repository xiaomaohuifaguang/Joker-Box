package com.cat.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.auth.mapper.ApiPathMapper;
import com.cat.auth.mapper.RoleMapper;
import com.cat.auth.service.ApiPathService;
import com.cat.common.entity.*;
import com.cat.common.entity.auth.ApiPath;
import com.cat.common.entity.auth.ApiPathGroup;
import com.cat.common.entity.auth.ApiPathPageParam;
import com.cat.common.entity.auth.ApiPathServer;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/***
 * api路径业务层实现
 * @title ApiPathServiceImpl
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/12 1:15
 **/
@Service
public class ApiPathServiceImpl implements ApiPathService {

    @Resource
    private ApiPathMapper apiPathMapper;
    @Resource
    private RoleMapper roleMapper;


    @Override
    public boolean save(ApiPath apiPath) {
        int flag;
        boolean exists = apiPathMapper.exists(new LambdaQueryWrapper<ApiPath>().eq(ApiPath::getPath, apiPath.getPath()).eq(ApiPath::getServer, apiPath.getServer()));
        if (!exists) {
            flag = apiPathMapper.insert(apiPath);
        } else {
            flag = apiPathMapper.update(new LambdaUpdateWrapper<ApiPath>()
                    .set(ApiPath::getName, apiPath.getName())
                    .set(ApiPath::getGroupName, apiPath.getGroupName())
                    .set(ApiPath::getWhiteList, apiPath.getWhiteList())
                    .set(ApiPath::getUpdateTime, LocalDateTime.now())
                    .eq(ApiPath::getPath, apiPath.getPath())
                    .eq(ApiPath::getServer, apiPath.getServer()));
        }
        return flag==1;
    }

    @Override
    @Transactional
    public boolean saveBatch(String server, List<ApiPath> apiPaths) {
        if(!StringUtils.hasText(server)) return false;
        boolean flag = true;
        List<String> paths = new ArrayList<>();
        for (ApiPath apiPath : apiPaths) {
            paths.add(apiPath.getPath());
            flag = flag && save(apiPath.setServer(server).setCreateTime(LocalDateTime.now()));
        }
        deleteBatch(server, paths);
        return flag;
    }

    @Override
    public void deleteBatch(String server, List<String> paths) {
        roleMapper.delete(server,paths);
        apiPathMapper.delete(new LambdaQueryWrapper<ApiPath>().eq(ApiPath::getServer, server).notIn(ApiPath::getPath, paths));
    }

    @Override
    public Page<ApiPath> queryPage(ApiPathPageParam pageParam) {
        Page<ApiPath> page = new Page<>(pageParam);
        page = apiPathMapper.selectPage(page,pageParam);
        return page;
    }

    @Override
    public List<SelectOption> selector(String server) {
        if(!StringUtils.hasText(server)){
            List<ApiPathServer> servers = apiPathMapper.servers();
            return servers.stream().map(apiPathServer -> new SelectOption(apiPathServer.getServer(), apiPathServer.getServer())).toList();
        }else {
            List<ApiPathGroup> groups = apiPathMapper.groups(server);
            return groups.stream().map(apiPathServer -> new SelectOption(apiPathServer.getGroupName(), apiPathServer.getGroupName())).toList();
        }
    }
}
