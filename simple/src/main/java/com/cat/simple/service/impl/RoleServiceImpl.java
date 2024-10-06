package com.cat.simple.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.simple.mapper.ApiPathMapper;
import com.cat.simple.mapper.RoleMapper;
import com.cat.simple.service.RoleService;
import com.cat.common.entity.CONSTANTS;
import com.cat.common.entity.DTO;
import com.cat.common.entity.Page;
import com.cat.common.entity.SelectOption;
import com.cat.common.entity.auth.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/***
 * 角色业务层实现
 * @title RoleServiceImpl
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/12 1:16
 **/
@Service
public class RoleServiceImpl implements RoleService {

    @Resource
    private RoleMapper roleMapper;
    @Resource
    private ApiPathMapper apiPathMapper;

    @Override
    public List<Role> getRoleByPath(String server, String path) {
        return roleMapper.getRoleByPath(server, path);
    }

    @Override
    public boolean allow(List<String> roleIds, String server, String path) {
        // 管理员权限 强制返回
        if(roleIds.contains("1")) return true;
        List<Role> roleByPath = getRoleByPath(server, path);
        for (Role role : roleByPath) {
            if(roleIds.contains(String.valueOf(role.getId()))){
                return true;
            }
        }
        return false;
    }

    @Override
    public Page<Role> queryPage(RolePageParam pageParam) {
        Page<Role> page = new Page<>(pageParam);
        page = roleMapper.selectPage(page,pageParam);
        return page;
    }

    @Override
    @Transactional
    public DTO<?> delete(Integer roleId) {
        if(roleId.equals(CONSTANTS.ROLE_ADMIN_CODE) || roleId.equals(CONSTANTS.ROLE_EVERYONE_CODE)){
            return DTO.error("该角色为默认角色，请不要删除");
        }
        Role role = roleMapper.selectById(roleId);
        if(ObjectUtils.isEmpty(role)){
            return DTO.error("角色不存在");
        }
        int withUser = roleMapper.withUser(roleId);
        if(withUser>0) return DTO.error("角色仍有关联用户，请先解除关联");
        int withApi = roleMapper.withApi(roleId);
        if(withApi>0) return DTO.error("角色仍有关联api，请先解除关联");
        int delete = roleMapper.deleteById(roleId);
        return delete > 0 ? DTO.success() : DTO.error("删除失败");
    }

    @Override
    @Transactional
    public boolean destroy(Integer roleId) {
        if(roleId.equals(CONSTANTS.ROLE_ADMIN_CODE) || roleId.equals(CONSTANTS.ROLE_EVERYONE_CODE)){
            return false;
        }
        int delete = roleMapper.deleteById(roleId);
        return delete > 0 ;
    }

    @Override
    public DTO<Role> add(String roleName) {
        if(!StringUtils.hasText(roleName)) return DTO.error("不可以",null);
        List<Role> roles = roleMapper.selectList(new LambdaQueryWrapper<Role>().eq(Role::getName, roleName));
        if(roles.size()>0){
            return DTO.error("不建议使用同名角色",null);
        }
        Role role = new Role().setName(roleName).setCreateTime(LocalDateTime.now());
        roleMapper.insert(role);
        return DTO.success(role);
    }

    @Override
    public List<ApiPathServer> apiPathTree(String roleId) {
        List<ApiPathServer> servers = apiPathMapper.servers();
        servers.forEach(s->{
            List<ApiPathGroup> groups = apiPathMapper.groups(s.getServer());
            groups.forEach(g->{
                List<ApiPath> apiPaths = apiPathMapper.selectListByRoleId(roleId,s.getServer(),g.getGroupName());
                g.setApiPaths(apiPaths);
            });
            s.setGroups(groups);
        });
        return servers;
    }

    @Override
    public Role info(String roleId) {
        return roleMapper.selectById(roleId);
    }

    @Override
    @Transactional
    public DTO<?> save(RoleAndApiPath roleAndApiPath) {
        Role role = roleAndApiPath.getRole();
        boolean exists = roleMapper.exists(new LambdaQueryWrapper<Role>().eq(Role::getId, role.getId()));
        if(!exists) {
            return DTO.error("角色不存在");
        }
        Role roleBase = roleMapper.selectById(role.getId());
        if(StringUtils.hasText(role.getName()) && !roleBase.getName().equals(role.getName())){
            List<Role> roles = roleMapper.selectList(new LambdaQueryWrapper<Role>().eq(Role::getName, role.getName()));
            if(roles.size()>0){
                return DTO.error("不建议使用同名角色",null);
            }
            roleMapper.update(new LambdaUpdateWrapper<Role>().set(Role::getName,role.getName()).set(Role::getUpdateTime,LocalDateTime.now()).eq(Role::getId,roleBase.getId()));
        }

        if(roleBase.getId().equals(CONSTANTS.ROLE_ADMIN_CODE)){
            return DTO.success();
        }

        List<Map<String,String>> roleApiRelation = new ArrayList<>();
        List<ApiPathServer> apiPathTree = roleAndApiPath.getApiPathTree();
        for (ApiPathServer apiPathServer : apiPathTree) {
            String server = apiPathServer.getServer();
            List<ApiPathGroup> groups = apiPathServer.getGroups();
            for (ApiPathGroup group : groups) {
                List<ApiPath> apiPaths = group.getApiPaths();
                for (ApiPath apiPath : apiPaths) {
                    if(apiPath.isRoleBind()){
                        roleApiRelation.add(new HashMap<>(){{
                            put("server",server);
                            put("apiPath",apiPath.getPath());
                        }});
                    }
                }
            }
        }

        roleMapper.deleteRoleApiRelation(roleBase.getId());
        if(roleApiRelation.size()>0){
            roleMapper.insertRoleApiRelation(roleBase.getId(),roleApiRelation,LocalDateTime.now());
            roleMapper.update(new LambdaUpdateWrapper<Role>().set(Role::getUpdateTime,LocalDateTime.now()).eq(Role::getId,roleBase.getId()));
        }
        return DTO.success();
    }

    @Override
    public List<SelectOption> selector() {
        List<Role> roles = roleMapper.selectList(new LambdaQueryWrapper<Role>().select(Role::getId, Role::getName));
        return roles.stream().map(role -> new SelectOption(role.getId(), role.getName())).collect(Collectors.toList());
    }
}
