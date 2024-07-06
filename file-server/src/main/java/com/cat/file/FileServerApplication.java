package com.cat.file;

import com.cat.file.config.minio.MinioService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;


/***
 * 文件服务主启动类
 * @title FileServerApplication
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/18 22:20
 **/
@SpringBootApplication
@EnableFeignClients(basePackages = {"com.cat.api"})
@Slf4j
public class FileServerApplication {

    @Resource
    private MinioService minioService;

    public static void main(String[] args) {
        log.info("服务正在启动...");
        SpringApplication.run(FileServerApplication.class, args);
        log.info("服务启动成功...");
    }


}
