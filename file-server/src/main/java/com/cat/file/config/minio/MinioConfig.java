package com.cat.file.config.minio;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/***
 * <TODO description class purpose>
 * @title MinioConfig
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/25 23:23
 **/
@Configuration
public class MinioConfig {

    @Value("${custom.minio.endpoint}")
    private String endpoint;

    @Value("${custom.minio.accessKey}")
    private String accessKey;

    @Value("${custom.minio.secretKey}")
    private String secretKey;


    @Bean
    public MinioClient minioClient(){
        return  MinioClient.builder()
                        .endpoint(endpoint)
                        .credentials(accessKey, secretKey)
                        .build();
    }



}
