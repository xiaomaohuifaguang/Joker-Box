package com.cat.auth.service;

import com.cat.common.entity.*;

import java.util.List;

/***
 * 角色业务层
 * @title RoleService
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/12 1:16
 **/
public interface RoleService {

    /**
     * 根据api路径获取角色
     * @param server 服务application.name
     * @param path api路径
     * @return 角色集合
     */
    List<Role> getRoleByPath(String server,String path);

    /**
     * 路径与角色匹配
     * 校验角色是否有权限调用当前接口路径
     * @param roleIds 角色id集合
     * @param server 服务名称application.name
     * @param path 路径
     * @return 校验结果
     */
    boolean allow(List<String> roleIds, String server,String path);

    /**
     * 角色列表
     * @param pageParam 查询参数包括分页参数
     * @return 分页结果
     */
    Page<Role> queryPage(RolePageParam pageParam);

    /**
     * 删除角色
     * @param roleId 角色id
     * @return 删除结果
     */
    DTO<?> delete(Integer roleId);

    /**
     * 暴力删除解除所有关联关系
     * @param roleId 角色id
     * @return 删除结果
     */
    boolean destroy(Integer roleId);

    /**
     * 增加角色
     * @param roleName 角色名称
     * @return 结果
     */
    DTO<Role> add(String roleName);

    /**
     * 所有服务及其向下所有分组所有api路径
     * @param roleId 角色id
     * @return 服务详细apiPath信息
     */
    List<ApiPathServer> apiPathTree(String roleId);

    /**
     * 所有服务及其向下所有分组所有api路径
     * @param roleId 角色id
     * @return 服务详细apiPath信息
     */
    Role info(String roleId);


    /**
     * 角色及详细信息保存
     * @param roleAndApiPath 角色详细信息
     * @return 结果
     */
    DTO<?> save(RoleAndApiPath roleAndApiPath);

    List<SelectOption> selector();


}
