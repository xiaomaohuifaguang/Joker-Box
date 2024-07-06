package com.cat.file.config.feign;

import com.cat.common.entity.CONSTANTS;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${custom.token}")
    private String token;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            template.header("Authorization", CONSTANTS.TOKEN_TYPE+" "+token);
        };
    }
}
