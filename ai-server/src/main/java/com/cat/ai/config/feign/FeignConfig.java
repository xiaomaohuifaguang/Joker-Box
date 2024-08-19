package com.cat.ai.config.feign;

import com.cat.common.entity.CONSTANTS;
import feign.RequestInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/***
 * Feign拦截
 * @title FeignConfig
 * @description Feign拦截
 * @author xiaomaohuifaguang
 * @create 2024/6/26 21:34
 **/
@Configuration
public class FeignConfig {


    @Resource
    private AuthUtils authUtils;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            if(!template.url().equals("/auth/getToken")){
                // 添加请求头
                template.header("Authorization", CONSTANTS.TOKEN_TYPE+" "+authUtils.getToken());
            }
        };
    }
}
