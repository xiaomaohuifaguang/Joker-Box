package com.cat.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/***
 * <TODO description class purpose>
 * @title DemoServerApplication
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/8/2 16:39
 **/
@SpringBootApplication
@EnableFeignClients(basePackages = {"com.cat.api"})
@Slf4j
public class DemoServerApplication {


    public static void main(String[] args) {
        log.info("服务正在启动...");
        SpringApplication.run(DemoServerApplication.class, args);
        log.info("服务启动成功...");
    }
}
