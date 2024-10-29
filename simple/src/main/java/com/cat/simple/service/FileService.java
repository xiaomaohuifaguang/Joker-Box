package com.cat.simple.service;

import com.cat.common.entity.DTO;
import com.cat.common.entity.file.FileInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/***
 * 文件服务业务层接口
 * @title FileService
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/26 23:23
 **/
public interface FileService {

    /**
     * 上传文件
     * @param uploadFile 上传文件
     * @return 文件信息
     */
    DTO<FileInfo> upload(MultipartFile uploadFile, String parentId) throws IOException;

    DTO<?> uploadAvatar(MultipartFile uploadFile) throws IOException;

    DTO<FileInfo> upload(MultipartFile uploadFile, String parentId, String realPath) throws IOException;

    /**
     * 创建文件夹
     * @param fileName 文件夹名称
     * @return 文件信息
     */
    DTO<FileInfo> createFolder(String fileName, String parentId) throws IOException;


    /**
     * 下载接口
     * @param fileId 文件唯一id
     */
    void download(String fileId) throws IOException;

    void downloadAvatar(String username) throws IOException;

    void download(String fileId, String realPath) throws IOException;


    DTO<List<FileInfo>> list(String parentId);

    DTO<?> delete(String fileId);

    DTO<?> rename(String fileId, String filename);


}