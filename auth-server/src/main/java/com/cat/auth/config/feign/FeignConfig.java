package com.cat.auth.config.feign;

import com.cat.auth.service.UserService;
import com.cat.common.entity.CONSTANTS;
import com.cat.common.entity.LoginInfo;
import feign.RequestInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/***
 * <TODO description class purpose>
 * @title FeignConfig
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/26 21:34
 **/
@Configuration
public class FeignConfig {

    @Resource
    private UserService userService;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            if(!template.url().equals("/auth/getToken")){
                String token = userService.getToken(new LoginInfo("admin", "admin", null));
                // 添加请求头
                template.header("Authorization", CONSTANTS.TOKEN_TYPE+" "+token);
            }
        };
    }
}
