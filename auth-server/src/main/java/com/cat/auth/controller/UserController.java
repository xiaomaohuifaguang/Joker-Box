package com.cat.auth.controller;

import com.cat.auth.service.UserService;
import com.cat.common.entity.*;
import com.cat.common.entity.auth.Role;
import com.cat.common.entity.auth.User;
import com.cat.common.entity.auth.UserPageParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/***
 * <TODO description class purpose>
 * @title UserController
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/17 14:12
 **/
@RestController
@RequestMapping("/user")
@Tag(name = "用户管理")
public class UserController {

    @Resource
    private UserService userService;


    @Operation(summary = "用户列表")
    @RequestMapping(value = "/queryPage",method = RequestMethod.POST)
    public HttpResult<Page<User>> queryPage(@RequestBody UserPageParam pageParam){
        Page<User> userPage = userService.queryPage(pageParam);
        return HttpResult.back(userPage);
    }

    @Operation(summary = "删除用户")
    @Parameters({
            @Parameter(name = "userId",description = "用户id",required = true)
    })
    @RequestMapping(value = "/delete",method = RequestMethod.POST)
    public HttpResult<?> delete(@RequestParam("userId") String userId){
        return HttpResult.back(userService.delete(userId));
    }

    @Operation(summary = "用户详细信息")
    @Parameters({
            @Parameter(name = "userId",description = "用户id",required = true)
    })
    @RequestMapping(value = "/userInfo",method = RequestMethod.POST)
    public HttpResult<User> userInfo(@RequestParam("userId") String userId){
        return HttpResult.back(userService.getUserInfo(userId));
    }

    @Operation(summary = "用户已绑定角色")
    @Parameters({
            @Parameter(name = "userId",description = "用户id",required = true)
    })
    @RequestMapping(value = "/roles",method = RequestMethod.POST)
    public HttpResult<List<Role>> roles(@RequestParam("userId") String userId){
        return HttpResult.back(userService.getRoleByUserId(userId));
    }

    @Operation(summary = "添加绑定角色")
    @Parameters({
            @Parameter(name = "userId",description = "用户id",required = true),
            @Parameter(name = "roleId",description = "角色id",required = true)
    })
    @RequestMapping(value = "/addRole",method = RequestMethod.POST)
    public HttpResult<?> addRole(@RequestParam("userId") String userId,@RequestParam("roleId") String roleId){
        return HttpResult.back(userService.addRole(userId,roleId));
    }

    @Operation(summary = "删除绑定角色")
    @Parameters({
            @Parameter(name = "userId",description = "用户id",required = true),
            @Parameter(name = "roleId",description = "角色id",required = true)
    })
    @RequestMapping(value = "/deleteRole",method = RequestMethod.POST)
    public HttpResult<?> deleteRole(@RequestParam("userId") String userId,@RequestParam("roleId") String roleId){
        return HttpResult.back(userService.deleteRole(userId,roleId));
    }

    @Operation(summary = "重置密码")
    @Parameters({
            @Parameter(name = "userId",description = "用户id",required = true)
    })
    @RequestMapping(value = "/resetPassword",method = RequestMethod.POST)
    public HttpResult<?> resetPassword(@RequestParam("userId") String userId){
        return HttpResult.back(userService.resetPassword(userId));
    }

}
