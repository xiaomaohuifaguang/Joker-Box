package com.cat.file.config.minio;

import io.minio.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.UUID;

/***
 * Minio 业务
 * @title MinioService
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/25 22:46
 **/
@Service
@Slf4j
public class MinioService {


    @Resource
    private MinioClient minioClient;

    public boolean isExists(String bucketName) {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * 创建 bucket
     * @param bucketName bucketName
     */
    public void createBucket(String bucketName) {
        try {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除 bucket
     * @param bucketName bucketName
     */
    public void removeBucket(String bucketName) {
        try {
            minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * 上传本地文件
     * @param bucketName bucketName
     * @param object 对象唯一名称
     * @param uploadFilePath 本地文件路径
     * @param contentType contentType取值最好在org.springframework.http.MediaType
     */
    public void uploadObject(String bucketName, String object, String uploadFilePath, String contentType) {
        try {
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(bucketName)
                            .object(object)
                            .contentType(contentType)
                            .filename(uploadFilePath)
                            .build());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * 下载对象到本地
     * @param bucketName bucketName
     * @param object 对象唯一名称
     * @param downloadFilePath 本地下载文件路径
     */
    public void downloadObject(String bucketName, String object, String downloadFilePath) {
        try {
            minioClient.downloadObject(DownloadObjectArgs.builder()
                    .bucket(bucketName)
                    .object(object)
                    .filename(downloadFilePath)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 流上传到minio
     * @param bucketName bucketName
     * @param object 对象唯一名称
     * @param inputStream 输入流
     * @param contentType contentType取值最好在org.springframework.http.MediaType
     */
    public String putObject(String bucketName, String object, InputStream inputStream, String contentType) {
        try {
            ObjectWriteResponse objectWriteResponse = minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(object).stream(
                                    inputStream, -1, 10485760)
                            .contentType(contentType)
                            .build());
            return objectWriteResponse.etag();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取对象流
     * @param bucketName bucketName
     * @param object 对象唯一名称
     * @return 对象输入流
     */
    public InputStream getObject(String bucketName, String object) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(object)
                            .build());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public InputStream getObject(String bucketName, String object, long offset, long length) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(object)
                            .offset(offset)
                            .length(length)
                            .build());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除对象
     * @param bucketName bucketName
     * @param object 对象唯一名称
     */
    public void removeObject(String bucketName, String object) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucketName).object(object).build());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


}
