package com.cat.simple.controller;

import com.cat.simple.service.RoleService;
import com.cat.common.entity.*;
import com.cat.common.entity.auth.ApiPathServer;
import com.cat.common.entity.auth.Role;
import com.cat.common.entity.auth.RoleAndApiPath;
import com.cat.common.entity.auth.RolePageParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/***
 * <TODO description class purpose>
 * @title RoleController
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/13 0:01
 **/
@RestController
@RequestMapping("/role")
@Tag(name = "角色管理")
public class RoleController {


    @Resource
    private RoleService roleService;


    @Operation(summary = "角色列表")
    @RequestMapping(value = "/queryPage", method = RequestMethod.POST)
    public HttpResult<Page<Role>> queryPage(@RequestBody RolePageParam pageParam){
        return HttpResult.back(roleService.queryPage(pageParam));
    }

    @Operation(summary = "删除角色")
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    @Parameters({
            @Parameter(name = "roleId", description = "角色id", required = true)
    })
    public HttpResult<?> delete(@RequestParam("roleId") Integer roleId){
        DTO<?> delete = roleService.delete(roleId);
        return HttpResult.back(delete);
    }

    @Operation(summary = "强制删除")
    @Parameters({
            @Parameter(name = "roleId", description = "角色id", required = true)
    })
    @RequestMapping(value = "/destroy", method = RequestMethod.POST)
    public HttpResult<?> destroy(@RequestParam("roleId") Integer roleId){
        return HttpResult.back(roleService.destroy(roleId) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "添加角色")
    @Parameters({
            @Parameter(name = "roleName", description = "角色名称", required = true)
    })
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public HttpResult<?> add(@RequestParam("roleName") String roleName){
        return HttpResult.back(roleService.add(roleName));
    }

    @Operation(summary = "api路径关系树")
    @RequestMapping(value = "/apiPathTree", method = RequestMethod.POST)
    public HttpResult<List<ApiPathServer>> apiPathTree(){
        return HttpResult.back(roleService.apiPathTree(""));
    }

    @Operation(summary = "角色关联api路径关系树")
    @Parameters({
            @Parameter(name = "roleId", description = "角色id", required = true)
    })
    @RequestMapping(value = "/apiPathTreeWithRole", method = RequestMethod.POST)
    public HttpResult<List<ApiPathServer>> apiPathTreeWithRole(@RequestParam("roleId") String roleId){
        return HttpResult.back(roleService.apiPathTree(roleId));
    }


    @Operation(summary = "角色信息")
    @Parameters({
            @Parameter(name = "roleId", description = "角色id", required = true)
    })
    @RequestMapping(value = "/info", method = RequestMethod.POST)
    public HttpResult<Role> info(@RequestParam("roleId") String roleId){
        return HttpResult.back(roleService.info(roleId));
    }

    @Operation(summary = "角色保存")
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public HttpResult<?> save(@RequestBody RoleAndApiPath roleAndApiPath){
        DTO<?> save = roleService.save(roleAndApiPath);
        return HttpResult.back(save);
    }

    @Operation(summary = "校验角色与路径是否匹配")
    @Parameters({
            @Parameter(name = "server", description = "application.name", required = true),
            @Parameter(name = "path", description = "api路径", required = true)
    })
    @RequestMapping(value = "/allow", method = RequestMethod.POST)
    public HttpResult<?> allow(@RequestBody List<String> userRoleIds,@RequestParam("server") String server, @RequestParam("path") String path){
        boolean allow = roleService.allow(userRoleIds, server, path);
        return HttpResult.back(allow ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR.setMsg("角色不在范伟内"));
    }

    @Operation(summary = "角色选择器")
    @RequestMapping(value = "/selector", method = RequestMethod.POST)
    public HttpResult<List<SelectOption>> selector(){
        return HttpResult.back(roleService.selector());
    }

}
