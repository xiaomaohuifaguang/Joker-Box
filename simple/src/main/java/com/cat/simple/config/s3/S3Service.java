package com.cat.simple.config.s3;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

/**
 * S3 业务（AWS SDK for Java v2）
 *
 * @title S3Service
 * @author xiaomaohuifaguang
 */
@Service
@Slf4j
public class S3Service {

    @Resource
    private S3Client s3Client;

    /**
     * 判断 bucket 是否存在
     */
    public boolean isExists(String bucketName) {
        try {
            HeadBucketResponse response = s3Client.headBucket(
                    HeadBucketRequest.builder().bucket(bucketName).build());
            return response.sdkHttpResponse().isSuccessful();
        } catch (NoSuchBucketException e) {
            return false;
        } catch (S3Exception e) {
            // 404 表示桶不存在
            if (e.statusCode() == 404) {
                return false;
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * 创建 bucket
     */
    public void createBucket(String bucketName) {
        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        } catch (BucketAlreadyOwnedByYouException e) {
            log.warn("Bucket [{}] already exists and is owned by you.", bucketName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除 bucket（必须先清空桶内对象）
     */
    public void removeBucket(String bucketName) {
        try {
            // 先删除桶内所有对象（v2 的 deleteBucket 要求桶为空）
            ListObjectsV2Response listResponse = s3Client.listObjectsV2(
                    ListObjectsV2Request.builder().bucket(bucketName).build());
            List<S3Object> objects = listResponse.contents();
            if (objects != null && !objects.isEmpty()) {
                for (S3Object obj : objects) {
                    s3Client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(obj.key())
                            .build());
                }
            }
            s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 上传本地文件
     *
     * @param bucketName     桶名
     * @param object         对象唯一名称
     * @param uploadFilePath 本地文件路径
     * @param contentType    如 org.springframework.http.MediaType 的取值
     */
    public void uploadObject(String bucketName, String object, String uploadFilePath, String contentType) {
        try {
            Path filePath = Paths.get(uploadFilePath);
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(object)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromFile(filePath));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 下载对象到本地
     */
    public void downloadObject(String bucketName, String object, String downloadFilePath) {
        try (ResponseInputStream<GetObjectResponse> is = s3Client.getObject(
                GetObjectRequest.builder().bucket(bucketName).key(object).build())) {
            File target = new File(downloadFilePath);
            // 确保父目录存在
            if (target.getParentFile() != null) {
                target.getParentFile().mkdirs();
            }
            java.nio.file.Files.copy(is, target.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 流上传到 S3
     *
     * @return etag
     */
    public String putObject(String bucketName, String object, InputStream inputStream, String contentType) {
        try {
            PutObjectResponse response = s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(object)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromInputStream(inputStream, inputStream.available()));
            return response.eTag();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取对象流
     */
    public InputStream getObject(String bucketName, String object) {
        try {
            return s3Client.getObject(
                    GetObjectRequest.builder().bucket(bucketName).key(object).build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取对象流（分段读取：从 offset 开始读 length 字节）
     */
    public InputStream getObject(String bucketName, String object, long offset, long length) {
        try {
            return s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(object)
                            .range("bytes=" + offset + "-" + (offset + length - 1))
                            .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除对象
     */
    public void removeObject(String bucketName, String object) {
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder().bucket(bucketName).key(object).build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取对象的 Base64（带 dataURI 前缀，可直接用于 <img src=...>）
     */
    public String getBase64(String bucketName, String object) {
        try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(
                GetObjectRequest.builder().bucket(bucketName).key(object).build())) {
            // 从响应头获取 Content-Type
            String mimeType = response.response().contentType();
            if (mimeType == null || mimeType.isEmpty()) {
                mimeType = "application/octet-stream";
            }
            byte[] fileBytes = response.readAllBytes(); // Java 9+
            return "data:" + mimeType + ";base64,"
                    + Base64.getEncoder().encodeToString(fileBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取对象的 Base64（不带 mimeType 前缀）
     */
    public String getBase64WithoutMimeType(String bucketName, String object) {
        try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(
                GetObjectRequest.builder().bucket(bucketName).key(object).build())) {
            byte[] fileBytes = response.readAllBytes(); // Java 9+
            return Base64.getEncoder().encodeToString(fileBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
