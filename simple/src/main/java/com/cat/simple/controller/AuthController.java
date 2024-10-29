package com.cat.simple.controller;

import com.cat.common.entity.file.FileInfo;
import com.cat.common.utils.RegexUtils;
import com.cat.simple.service.UserService;
import com.cat.common.entity.DTO;
import com.cat.common.entity.HttpResult;
import com.cat.common.entity.HttpResultStatus;
import com.cat.common.entity.auth.LoginInfo;
import com.cat.common.entity.auth.LoginUser;
import com.cat.common.entity.auth.RegisterUserInfo;
import com.cat.common.entity.auth.UserInfo;
import freemarker.template.TemplateException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
        return HttpResult.back(StringUtils.hasText(token) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR_USERNAME_OR_PASSWORD, token);
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
        if( RegexUtils.validate(mail, RegexUtils.EMAIL_REGEX)){
            userService.code(mail);
            return HttpResult.back( HttpResultStatus.SUCCESS);
        }else {
            return HttpResult.back( HttpResultStatus.ERROR).setMsg("邮箱格式不正确");
        }

    }

    @Operation(summary = "注册")
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public HttpResult<?> register(@RequestBody RegisterUserInfo registerUserInfo){
        DTO<?> register = userService.register(registerUserInfo);
        return HttpResult.back(register.flag ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR).setMsg(register.msg);
    }

    @Operation(summary = "上传文件")
    @Parameters({
            @Parameter(name = "uploadFile", schema = @Schema(format = "binary"), description = "文件",required = true),
    })
    @RequestMapping(value = "/avatarUpload", method = RequestMethod.POST)
    public HttpResult<?> upload(@RequestPart(value = "uploadFile") MultipartFile uploadFile) throws IOException {
        return HttpResult.back(userService.avatarUpload(uploadFile) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }


    @Operation(summary = "头像")
    @Parameters({
            @Parameter(name = "username", description = "用户名", required = true, in = ParameterIn.PATH)
    })
    @RequestMapping(value = "/avatar/{username}", method = RequestMethod.GET)
    public void avatar( @PathVariable String username) throws IOException {
        userService.avatar(username);
    }

    @Operation(summary = "修改密码")
    @Parameters({
            @Parameter(name = "oldPassword", description = "原密码", required = true),
            @Parameter(name = "newPassword", description = "新密码", required = true)
    })
    @RequestMapping(value = "/changePassword", method = RequestMethod.POST)
    public HttpResult<?> changePassword( @RequestParam("oldPassword") String oldPassword, @RequestParam("newPassword") String newPassword) throws IOException {
        return HttpResult.back(userService.changePassword(oldPassword, newPassword));
    }


}
