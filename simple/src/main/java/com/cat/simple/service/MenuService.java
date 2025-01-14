package com.cat.simple.service;


import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.auth.ApiPathServer;
import com.cat.common.entity.menu.Menu;
import com.cat.common.entity.menu.MenuAndApiPath;

import java.util.List;

public interface MenuService {

    boolean add(Menu menu);

    boolean delete(Menu menu);

    boolean update(Menu menu);

    boolean save(MenuAndApiPath menuAndApiPath);

    Menu info(Menu menu);

    Page<Menu> queryPage(PageParam pageParam);


    List<Menu> queryAllByAuth(Integer menuType);

    List<Integer> queryMenuChoose( Integer roleId, Integer menuType);

    List<Menu> queryAll(Integer menuType);

    List<ApiPathServer> apiPathTree(String menuId);

}