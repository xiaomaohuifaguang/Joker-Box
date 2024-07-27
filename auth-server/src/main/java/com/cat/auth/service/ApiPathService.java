package com.cat.auth.service;

import com.cat.common.entity.ApiPath;
import com.cat.common.entity.ApiPathPageParam;
import com.cat.common.entity.Page;
import com.cat.common.entity.SelectOption;

import java.util.List;

/***
 * api路径业务层
 * @title ApiPathService
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/12 1:15
 **/
public interface ApiPathService {

    /**
     * 保存api 插入/更新
     * @param apiPath api路径信息
     * @return 操作情况
     */
    boolean save(ApiPath apiPath);

    /**
     * 全量写入
     * @param server 服务 application.name
     * @param apiPaths 服务全部的api路径信息
     * @return 操作情况
     */
    boolean saveBatch(String server, List<ApiPath> apiPaths);

    /**
     * 删除server服务注册的不在list中的 api路径
     * @param server 服务 application.name
     * @param paths api路径集合
     */
    void deleteBatch(String server, List<String> paths);

    Page<ApiPath> queryPage(ApiPathPageParam pageParam);

    /**
     * api选择器
     * @return 选择器
     */
    List<SelectOption> selector(String server);



}
