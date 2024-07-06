package com.cat.file.service;

import com.cat.common.entity.FileInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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
    FileInfo upload(MultipartFile uploadFile) throws IOException;


    /**
     * 下载接口
     * @param fileId 文件唯一id
     */
    void download(String fileId) throws IOException;


}
