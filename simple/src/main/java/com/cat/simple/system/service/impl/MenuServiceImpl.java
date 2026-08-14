package com.cat.simple.system.service.impl;

import com.cat.common.entity.Page;
import com.cat.common.entity.auth.*;
import com.cat.common.entity.menu.Menu;
import com.cat.common.entity.menu.MenuAndApiPath;
import com.cat.common.entity.menu.MenuPageParam;
import com.cat.simple.config.cache.CacheKeyEnum;
import com.cat.simple.config.cache.CacheService;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.apidoc.mapper.ApiPathMapper;
import com.cat.simple.system.mapper.MenuMapper;
import com.cat.simple.system.service.MenuService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.cat.common.entity.CONSTANTS.ROLE_ADMIN_CODE;

@Service
public class MenuServiceImpl implements MenuService {


    @Resource
    private MenuMapper menuMapper;
    @Resource
    private ApiPathMapper apiPathMapper;
    @Resource
    private CacheService cacheService;

    @Override
    public boolean add(Menu menu) {
        menu.setUserId(Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId());
        boolean success = menuMapper.insert(menu) == 1;
        if (success) {
            // 菜单结构变更影响所有角色的菜单缓存，一键清空，读取时按角色懒重建
            cacheService.deleteKey(CacheKeyEnum.ROLE_MENUS);
        }
        return success;
    }

    @Override
    public boolean delete(Menu menu) {
        boolean success = menuMapper.deleteById(menu) == 1;
        if (success) {
            cacheService.deleteKey(CacheKeyEnum.ROLE_MENUS);
        }
        return success;
    }

    @Override
    public boolean update(Menu menu) {
        menu.setUpdateTime(LocalDateTime.now());
        boolean success = menuMapper.updateById(menu) == 1;
        if (success) {
            cacheService.deleteKey(CacheKeyEnum.ROLE_MENUS);
        }
        return success;
    }

    @Override
    public boolean save(MenuAndApiPath menuAndApiPath) {

        List<HashMap<String, String>> roleApiRelation = new ArrayList<>();
        List<ApiPathServer> apiPathTree = menuAndApiPath.getApiPathTree();
        for (ApiPathServer apiPathServer : apiPathTree) {
            String server = apiPathServer.getServer();
            List<ApiPathGroup> groups = apiPathServer.getGroups();
            for (ApiPathGroup group : groups) {
                List<ApiPath> apiPaths = group.getApiPaths();
                for (ApiPath apiPath : apiPaths) {
//                    if(apiPath.isRoleBind()){
//                        roleApiRelation.add(new HashMap<>(){{
//                            put("server",server);
//                            put("apiPath",apiPath.getPath());
//                        }});
//                    }
                    roleApiRelation.add(new HashMap<>() {{
                        put("server", server);
                        put("apiPath", apiPath.getPath());
                    }});
                }
            }
        }

        menuMapper.deleteMenuApiRelation(menuAndApiPath.getMenu().getId());
        if (!roleApiRelation.isEmpty()) {
            menuMapper.insertMenuApiRelation(menuAndApiPath.getMenu().getId(), roleApiRelation, LocalDateTime.now());
        }
        return update(menuAndApiPath.getMenu());
    }

    @Override
    public Menu info(Menu menu) {
        return menuMapper.selectById(menu.getId());
    }

    @Override
    public Page<Menu> queryPage(MenuPageParam pageParam) {
        Page<Menu> page = new Page<>(pageParam);
        page = menuMapper.selectPage(page, pageParam);
        return page;
    }

    @Override
    public List<Menu> queryAllByAuth(Integer menuType) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        List<Integer> roleIds = new ArrayList<>();
        if (!ObjectUtils.isEmpty(loginUser)) {
            List<Role> roles = loginUser.getRoles();
            roleIds = roles.stream().map(Role::getId).toList();
        }

        // 获取全部有权限的菜单
//        List<Menu> menus = new ArrayList<>(); // menuMapper.queryAllByAuth(roleIds, roleIds.contains(ROLE_ADMIN_CODE));
        LinkedHashSet<Menu> menus = new LinkedHashSet<>();
        if(roleIds.contains(ROLE_ADMIN_CODE)){
            List<Menu> adminMenuList = cacheService.hgetList(CacheKeyEnum.ROLE_MENUS, String.valueOf(ROLE_ADMIN_CODE), Menu.class);
            if(CollectionUtils.isEmpty(adminMenuList)){
                adminMenuList = menuMapper.queryAllByAuth(null, true);
                cacheService.hset(CacheKeyEnum.ROLE_MENUS, String.valueOf(ROLE_ADMIN_CODE), adminMenuList);
            }
            menus.addAll(adminMenuList);
        }else {
            roleIds.forEach(roleId -> {
                List<Menu> roleMenuList = cacheService.hgetList(CacheKeyEnum.ROLE_MENUS, String.valueOf(roleId), Menu.class);
                if(CollectionUtils.isEmpty(roleMenuList)){
                    roleMenuList = menuMapper.queryAllByAuth(List.of(roleId), false);
                    cacheService.hset(CacheKeyEnum.ROLE_MENUS, String.valueOf(roleId), roleMenuList);
                    menus.addAll(roleMenuList);
                }
                menus.addAll(roleMenuList);
            });
        }



        return getMenuTree(new ArrayList<>(menus), menuType);
    }

    @Override
    public List<Integer> queryMenuChoose(Integer roleId, Integer menuType) {

        // 获取全部有权限的菜单
        List<Menu> menus = menuMapper.queryAllByAuth(Collections.singletonList(roleId), roleId.equals(ROLE_ADMIN_CODE));
        List<Menu> queryAll = queryAll(menuType);
        List<Integer> list = menus.stream().map(Menu::getId).toList();
        return recursion(queryAll, list);
    }

    @Override
    public List<Menu> queryAll(Integer menuType) {


        // 获取全部有权限的菜单
        List<Menu> menus = menuMapper.queryAllByAuth(null, true);


        return getMenuTree(menus, menuType);
    }

    @Override
    public List<ApiPathServer> apiPathTree(String menuId) {
        List<ApiPathServer> servers = apiPathMapper.servers();
        servers.forEach(s -> {
            List<ApiPathGroup> groups = apiPathMapper.groups(s.getServer());
            groups.forEach(g -> {
                List<ApiPath> apiPaths = apiPathMapper.selectListByMenuId(menuId, s.getServer(), g.getGroupName());
                g.setApiPaths(apiPaths);
            });
            s.setGroups(groups);
        });
        return servers;
    }

    @Override
    public List<String> queryAllPathByAuth() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if(Objects.isNull(loginUser) || CollectionUtils.isEmpty(loginUser.getRoles())){
            return new ArrayList<>();
        }
        List<Role> roles = loginUser.getRoles();
        List<Integer> roleIds = roles.stream().map(Role::getId).toList();

        // 获取全部有权限的菜单
        List<Menu> menus = menuMapper.queryAllByAuth(roleIds, roleIds.contains(ROLE_ADMIN_CODE));

        return menus.stream().map(Menu::getPath).toList();
    }


    private List<Menu> getOrDefaultEmptyList(Map<Integer, List<Menu>> map, Integer key) {
        List<Menu> list = map.get(key);
        return CollectionUtils.isEmpty(list) ? new ArrayList<>() : list;
    }

    private List<Menu> getChild(Integer parentId, Map<Integer, List<Menu>> groupMap) {
        List<Menu> menuList = getOrDefaultEmptyList(groupMap, parentId);
        menuList.forEach(menu -> menu.setChildren(getChild(menu.getId(), groupMap)));
        return menuList;
    }

    private List<Menu> getMenuTree(List<Menu> menus, Integer parentId) {
        // 分组
        Map<Integer, List<Menu>> collect = menus.stream().collect(Collectors.groupingBy(Menu::getParentId));
        // 获取第一组 并向下取子菜单
        List<Menu> result = getOrDefaultEmptyList(collect, parentId);
        result.forEach(m -> m.setChildren(getChild(m.getId(), collect)));
        return result;
    }


    private List<Integer> recursion(List<Menu> menus, List<Integer> choose) {
        List<Integer> result = new ArrayList<>();
        menus.forEach(menu -> {
            if (choose.contains(menu.getId())) {
                result.add(menu.getId());
            }
            List<Integer> recursion = recursion(menu.getChildren(), choose);
            result.addAll(recursion);
        });
        return result;
    }
}