package com.cat.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;


/***
 * 鉴权服务主启动类
 * @title AuthServerApplication
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/18 22:20
 **/
@SpringBootApplication
@EnableFeignClients(basePackages = {"com.cat.api"})
@EnableScheduling
@Slf4j
public class AuthServerApplication {



    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        log.info("服务正在启动...");
        SpringApplication.run(AuthServerApplication.class, args);
        log.info("服务启动成功：耗时"+(System.currentTimeMillis() - startTime) + "ms");
    }



}
