package com.cat.simple.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.common.entity.*;
import com.cat.common.entity.file.FileInfo;
import com.cat.common.utils.CatUUID;
import com.cat.common.utils.IOUtils;
import com.cat.common.utils.ServletUtils;
import com.cat.simple.config.minio.MinioService;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.mapper.FileInfoMapper;
import com.cat.simple.mapper.UserMapper;
import com.cat.simple.service.FileService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;


/***
 * 文件服务业务层接口实现
 * @title FileServiceImpl
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/26 23:24
 **/
@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Value("${custom.minio.bucketName}")
    private String BUCKET_NAME;

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Resource
    private MinioService minioService;

    @Resource
    private ResourceLoader resourceLoader;

    @Resource
    private UserMapper userMapper;

    private final static String UPLOAD_PATH = "/码头云盘/";

    @Override
    @Transactional
    public DTO<FileInfo> upload(MultipartFile uploadFile, String parentId) throws IOException {
        return upload(uploadFile, parentId, UPLOAD_PATH+SecurityUtils.getLoginUser().getUsername()+"/");
    }

    @Override
    public DTO<?> uploadAvatar(MultipartFile uploadFile) throws IOException {
        long size = uploadFile.getSize();
        if(!SecurityUtils.isAdmin() && ( size >  100 * 1000 * 1000) ){
            return DTO.error("只有尊贵的VIP才能上传超过100M的文件",null);
        }

        String filename = CatUUID.randomUUID();
        String realFilename = "/头像/"+SecurityUtils.getLoginUser().getUsername();
        new Thread(()->{
            try {
                minioService.putObject(BUCKET_NAME, realFilename, uploadFile.getInputStream(), uploadFile.getContentType());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
        return DTO.success();
    }

    @Override
    public DTO<FileInfo> upload(MultipartFile uploadFile, String parentId, String realPath) throws IOException {
        long size = uploadFile.getSize();
        if(!SecurityUtils.isAdmin() && ( size >  100 * 1000 * 1000) ){
            return DTO.error("只有尊贵的VIP才能上传超过100M的文件",null);
        }

        if(!parentId.equals(CONSTANTS.FILE_ALL_PARENT) && notExistFolder(parentId)){
            return DTO.error("文件夹不存在",null);
        }

        FileInfo fileInfo = new FileInfo()
                .setId(CatUUID.randomUUID())
                .setFilename(uploadFile.getOriginalFilename())
                .setType(CONSTANTS.FILE_TYPE_1)
                .setParentId(parentId)
                .setSize(uploadFile.getSize())
                .setContentType(uploadFile.getContentType())
                .setUserId(Integer.parseInt(SecurityUtils.getLoginUser().getUserId()));
        new Thread(()->{
            try {
                minioService.putObject(BUCKET_NAME, realPath+fileInfo.getId(), uploadFile.getInputStream(), uploadFile.getContentType());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
        return fileInfoMapper.insert(fileInfo) == 1
                ? DTO.success(fileInfo) : DTO.error("上传失败",null);
    }


    @Override
    @Transactional
    public DTO<FileInfo> createFolder(String fileName, String parentId) throws IOException {

        if(!StringUtils.hasText(fileName)){
            return DTO.error("文件夹名称不能为空",null);
        }
        if(!parentId.equals(CONSTANTS.FILE_ALL_PARENT) && notExistFolder(parentId)){
            return DTO.error("父级文件夹不存在",null);
        }
        Long fFolderCount = fileInfoMapper.selectCount(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getUserId, SecurityUtils.getLoginUser().getUserId()).eq(FileInfo::getFilename, fileName).eq(FileInfo::getType,CONSTANTS.FILE_TYPE_2));
        if(fFolderCount > 0){
            return DTO.error("文件夹已存在，列表不好看",null);
        }
        FileInfo fileInfo = new FileInfo()
                .setId(CatUUID.randomUUID())
                .setFilename(fileName)
                .setType(CONSTANTS.FILE_TYPE_2)
                .setParentId(parentId)
                .setUserId(Integer.parseInt(SecurityUtils.getLoginUser().getUserId()));
        fileInfoMapper.insert(fileInfo);
        return DTO.success(fileInfo);
    }

    @Override
    public void download(String fileId) throws IOException {
        download(fileId, UPLOAD_PATH+userMapper.selectById(fileInfoMapper.selectById(fileId).getUserId()).getUsername()+"/");

    }

    @Override
    public void downloadAvatar(String username) throws IOException {
        HttpServletResponse response = ServletUtils.getHttpServletResponse();
        HttpServletRequest request = ServletUtils.getHttpServletRequest();
        InputStream inputStream = null;
        try {
            // 设置响应头
            response.setContentType("application/octet-stream");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + URLEncoder.encode(username, StandardCharsets.UTF_8) + "\"");
            // 获取 Range 头部信息
            String rangeHeader = request.getHeader(HttpHeaders.RANGE);
            // 如果没有 Range 头部，则直接返回整个文件内容
            inputStream = minioService.getObject(BUCKET_NAME, "/头像/"+username);
            OutputStream outputStream = response.getOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }catch (ClientAbortException clientAbortException){
            log.info(clientAbortException.getMessage());
        }finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    @Override
    public void download(String fileId, String realPath) throws IOException {
        FileInfo fileInfo = fileInfoMapper.selectById(fileId);
        HttpServletResponse response = ServletUtils.getHttpServletResponse();
        HttpServletRequest request = ServletUtils.getHttpServletRequest();
        if (!ObjectUtils.isEmpty(fileInfo)) {
            InputStream inputStream = null;
            try {
                long fileLength = fileInfo.getSize();
                // 设置响应头
                response.setContentType(fileInfo.getContentType());
                response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileLength));
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + URLEncoder.encode(fileInfo.getFilename(), StandardCharsets.UTF_8) + "\"");
                // 获取 Range 头部信息
                String rangeHeader = request.getHeader(HttpHeaders.RANGE);
                if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                    // 解析 Range 头部，获取开始和结束位置
                    String[] ranges = rangeHeader.substring("bytes=".length()).split("-");
                    long start = Long.parseLong(ranges[0]);
                    long end = ranges.length > 1 && !ranges[1].isEmpty() ? Long.parseLong(ranges[1]) : fileLength - 1;
                    // 设置响应状态为部分内容返回
                    response.setStatus(HttpStatus.PARTIAL_CONTENT.value());
                    response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength);
                    // 计算需要返回的字节数
                    long contentLength = end - start + 1;
                    response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
                    inputStream = minioService.getObject(BUCKET_NAME, realPath+fileId, start, end - start + 1);
                    // 写入输出流
                    OutputStream outputStream = response.getOutputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                } else {
                    // 如果没有 Range 头部，则直接返回整个文件内容
                    inputStream = minioService.getObject(BUCKET_NAME, realPath+fileId);
                    OutputStream outputStream = response.getOutputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                }
            }catch (ClientAbortException clientAbortException){
                log.info(clientAbortException.getMessage());
            }finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } else {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            response.setContentType(MediaType.IMAGE_PNG_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + URLEncoder.encode("404.png", String.valueOf(StandardCharsets.UTF_8)) + "\"");
            IOUtils.saveStream(resourceLoader.getResource("classpath:/static/img/404.png").getInputStream(), response.getOutputStream());
        }
    }

    @Override
    public DTO<List<FileInfo>> list(String parentId) {
        if(!parentId.equals(CONSTANTS.FILE_ALL_PARENT) && notExistFolder(parentId)){
            return DTO.error("父级文件夹不存在",null);
        }
        List<FileInfo> fileInfos = fileInfoMapper.selectList(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getUserId, SecurityUtils.getLoginUser().getUserId())
                .eq(FileInfo::getParentId, parentId).orderByDesc(FileInfo::getType,FileInfo::getCreateTime)
                .and(c->{
                    c.eq(FileInfo::getType, CONSTANTS.FILE_TYPE_1).or().eq(FileInfo::getType, CONSTANTS.FILE_TYPE_2);
                })
        );
        return DTO.back(fileInfos);
    }

    @Override
    @Transactional
    public DTO<?> delete(String fileId) {
        FileInfo fileInfo = fileInfoMapper.selectOne(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getUserId, SecurityUtils.getLoginUser().getUserId()).eq(FileInfo::getId, fileId));
        if(ObjectUtils.isEmpty(fileInfo)){
            return DTO.error("文件不存在");
        }
        if(fileInfo.getType().equals(CONSTANTS.FILE_TYPE_2) && fileInfoMapper.selectCount(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getUserId, SecurityUtils.getLoginUser().getUserId()).eq(FileInfo::getParentId, fileInfo.getId())) > 0){
            return DTO.error("请先删除文件夹里内容");
        }

        fileInfoMapper.deleteById(fileId);
        minioService.removeObject(BUCKET_NAME,UPLOAD_PATH+userMapper.selectById(fileInfo.getUserId()).getUsername()+"/"+fileInfo.getId());
        return DTO.success();
    }

    @Override
    @Transactional
    public DTO<?> rename(String fileId, String filename) {
        if(!StringUtils.hasText(filename)){
            return DTO.error("文件夹名称不能为空",null);
        }
        FileInfo fileInfo = fileInfoMapper.selectOne(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getUserId, SecurityUtils.getLoginUser().getUserId()).eq(FileInfo::getId, fileId));
        if(ObjectUtils.isEmpty(fileInfo)){
            return DTO.error("文件不存在");
        }
        fileInfoMapper.update(new LambdaUpdateWrapper<FileInfo>().set(FileInfo::getFilename,filename).eq(FileInfo::getUserId, SecurityUtils.getLoginUser().getUserId()).eq(FileInfo::getId, fileId));
        return DTO.success();
    }


    private boolean notExistFolder(String folderId){
        Long parentFolderCount = fileInfoMapper.selectCount(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getUserId, SecurityUtils.getLoginUser().getUserId()).eq(FileInfo::getId, folderId).eq(FileInfo::getType,CONSTANTS.FILE_TYPE_2));
        return parentFolderCount != 1;
    }

}