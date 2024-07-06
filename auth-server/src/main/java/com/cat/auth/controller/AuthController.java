package com.cat.auth.controller;

import com.cat.api.file.FileServiceClient;
import com.cat.auth.service.UserService;
import com.cat.common.entity.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/***
 * <TODO description class purpose>
 * @title AuthController
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/24 22:58
 **/
@RestController
@RequestMapping("/auth")
@Tag(name = "鉴权服务")
public class AuthController {

    @Resource
    private FileServiceClient fileServiceClient;

    @Resource
    private UserService userService;

    @Operation(summary = "获取token")
    @RequestMapping(value = "/getToken", method = RequestMethod.POST)
    public HttpResult<String> getToken(@RequestBody LoginInfo loginInfo){
        String token = userService.getToken(loginInfo);
        return HttpResult.back(StringUtils.hasText(token) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR, token);
    }

    @Operation(summary = "令牌鉴权")
    @RequestMapping(value = "/getLoginUserByToken", method = RequestMethod.POST)
    public HttpResult<LoginUser> getLoginUser(@RequestBody LoginInfo loginInfo){
        return HttpResult.back(userService.getLoginUserByToken(loginInfo.getToken()));
    }

}
