package com.cat.auth.controller;

import com.cat.auth.service.UserService;
import com.cat.common.entity.*;
import freemarker.template.TemplateException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

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
        LoginUser loginUserByToken = userService.getLoginUserByToken(loginInfo.getToken());
        return HttpResult.back(ObjectUtils.isEmpty(loginUserByToken) ? HttpResultStatus.ERROR : HttpResultStatus.SUCCESS ,loginUserByToken);
    }

    @Operation(summary = "用户信息")
    @RequestMapping(value = "/userInfo", method = RequestMethod.POST)
    public HttpResult<UserInfo> userInfo(){
        UserInfo userInfo = userService.getUserInfo();
        return HttpResult.back(ObjectUtils.isEmpty(userInfo) ? HttpResultStatus.ERROR : HttpResultStatus.SUCCESS ,userInfo);
    }

    @Operation(summary = "验证码")
    @Parameters({
            @Parameter(name = "mail", description = "邮箱",required = true)
    })
    @RequestMapping(value = "/mailCode", method = RequestMethod.POST)
    public HttpResult<?> mailCode(@RequestParam("mail") String mail) throws TemplateException, MessagingException, IOException {
        userService.code(mail);
        return HttpResult.back(HttpResultStatus.SUCCESS);
    }

    @Operation(summary = "注册")
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public HttpResult<?> register(@RequestBody RegisterUserInfo registerUserInfo){
        DTO<?> register = userService.register(registerUserInfo);
        return HttpResult.back(register.flag ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR).setMsg(register.msg);
    }




}
