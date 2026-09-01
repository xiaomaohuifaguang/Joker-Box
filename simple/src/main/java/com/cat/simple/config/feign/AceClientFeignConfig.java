package com.cat.simple.config.feign;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;


public class AceClientFeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            template.header("X-Api-Key", "joker-box-ace-11111111");
        };
    }


}
