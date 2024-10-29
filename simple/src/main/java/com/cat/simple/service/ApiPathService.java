package com.cat.simple.service;

import com.cat.common.entity.Cascade;
import com.cat.common.entity.Page;
import com.cat.common.entity.SelectOption;
import com.cat.common.entity.auth.ApiPath;
import com.cat.common.entity.auth.ApiPathPageParam;

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

    boolean update(ApiPath apiPath);

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

    /**
     * 所有服务及其向下所有分组所有api路径
     * @param server 服务名称 application.name
     * @param path 路径 api路径
     * @return 服务详细apiPath信息
     */
    ApiPath info(String server, String path);

    /**
     * api级联到分组
     * @return api级联到分组信息
     */
    List<Cascade> cascadeServerGroup();

}
