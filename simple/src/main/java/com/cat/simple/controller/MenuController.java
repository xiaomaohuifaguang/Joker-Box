package com.cat.simple.controller;

import com.cat.common.entity.*;
import com.cat.common.entity.auth.ApiPathServer;
import com.cat.common.entity.menu.Menu;
import com.cat.common.entity.menu.MenuAndApiPath;
import com.cat.simple.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/menu")
@Tag(name = "菜单管理")
public class MenuController {

@Resource
private MenuService menuService;

    @Operation(summary = "添加")
    @RequestMapping(value = "/add",method = RequestMethod.POST)
    public HttpResult<?> add(@RequestBody Menu menu) {
        if(!menu.verify()){
            return HttpResult.back(HttpResultStatus.ERROR);
        }
        return HttpResult.back(menuService.add(menu) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "删除")
    @RequestMapping(value = "/remove",method = RequestMethod.POST)
    public HttpResult<?> remove(@RequestBody Menu menu) {
        return HttpResult.back(menuService.delete(menu) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "修改")
    @RequestMapping(value = "/update",method = RequestMethod.POST)
    public HttpResult<?> update(@RequestBody Menu menu) {
        if(!menu.verify()){
            return HttpResult.back(HttpResultStatus.ERROR);
        }
        return HttpResult.back(menuService.update(menu) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "保存")
    @RequestMapping(value = "/save",method = RequestMethod.POST)
    public HttpResult<?> update(@RequestBody MenuAndApiPath menuAndApiPath) {
        if(!menuAndApiPath.getMenu().verify()){
            return HttpResult.back(HttpResultStatus.ERROR);
        }
        return HttpResult.back(menuService.save(menuAndApiPath) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "详情")
    @RequestMapping(value = "/info",method = RequestMethod.POST)
    public HttpResult<Menu> info(@RequestBody Menu menu) {
        return HttpResult.back(menuService.info(menu));
    }

    @Operation(summary = "分页")
    @RequestMapping(value = "/queryPage",method = RequestMethod.POST)
    public HttpResult<Page<Menu>> queryPage(@RequestBody PageParam pageParam) {
        return HttpResult.back(menuService.queryPage(pageParam));
    }

    @Operation(summary = "具有权限菜单树")
    @RequestMapping(value = "/menuTree",method = { RequestMethod.POST, RequestMethod.GET })
    @Parameters({
            @Parameter(name = "menuType",description = "菜单类型")
    })
    public HttpResult<List<Menu>> menuTree(@RequestParam(value = "menuType",defaultValue = "-1") Integer menuType) {
        return HttpResult.back(menuService.queryAllByAuth(menuType));
    }

    @Operation(summary = "菜单树全部")
    @RequestMapping(value = "/menuTreeAll",method = { RequestMethod.POST, RequestMethod.GET })
    @Parameters({
            @Parameter(name = "menuType",description = "菜单类型")
    })
    public HttpResult<List<Menu>> menuTreeAll(@RequestParam(value = "menuType",defaultValue = "-1") Integer menuType) {
        return HttpResult.back(menuService.queryAll(menuType));
    }


    @Operation(summary = "菜单权限id集合")
    @RequestMapping(value = "/menuChoose",method = { RequestMethod.POST, RequestMethod.GET })
    @Parameters({
            @Parameter(name = "roleId",description = "角色id",required = true),
            @Parameter(name = "menuType",description = "菜单类型")
    })
    public HttpResult<List<Integer>> queryMenuChoose(@RequestParam(value = "roleId") Integer roleId, @RequestParam(value = "menuType",defaultValue = "-1") Integer menuType) {
        return HttpResult.back(menuService.queryMenuChoose(roleId, menuType));
    }

    @Operation(summary = "菜单关联api路径关系树")
    @Parameters({
            @Parameter(name = "menuId", description = "菜单id", required = true)
    })
    @RequestMapping(value = "/apiPathTreeWithMenu", method = RequestMethod.POST)
    public HttpResult<List<ApiPathServer>> apiPathTreeWithMenu(@RequestParam("menuId") String menuId){
        return HttpResult.back(menuService.apiPathTree(menuId));
    }

}