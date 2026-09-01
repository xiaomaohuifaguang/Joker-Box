package com.cat.simple.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.common.entity.*;
import com.cat.common.entity.file.FileInfo;
import com.cat.common.utils.CatUUID;
import com.cat.common.utils.IOUtils;
import com.cat.common.utils.ServletUtils;
import com.cat.common.utils.UUIDUtils;
import com.cat.simple.config.minio.MinioService;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.file.mapper.FileInfoMapper;
import com.cat.simple.system.mapper.UserMapper;
import com.cat.simple.file.service.FileService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;


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

    @Value("${minio.bucketName}")
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

    private final static String DYNAMIC_FORM = "/动态表单/";

    private final static String AGENT_FILE = "/AGENT_FILE/";

    @Value("${custom.file.local-tmp}")
    private String LOCAL_FILE_TMP;

    @Override
    @Transactional
    public DTO<FileInfo> upload(MultipartFile uploadFile, String parentId) throws IOException {
        if(!parentId.equals(CONSTANTS.FILE_ALL_PARENT) && notExistFolder(parentId)){
            return DTO.error("文件夹不存在",null);
        }
        String userId = Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId();
        return upload(uploadFile, parentId, UPLOAD_PATH+SecurityUtils.getLoginUser().getUserId()+"/", userId);
    }

    @Override
    public DTO<?> uploadAvatar(MultipartFile uploadFile) throws IOException {
        long size = uploadFile.getSize();
        if(!SecurityUtils.isAdmin() && ( size >  100 * 1000 * 1000) ){
            return DTO.error("只有尊贵的VIP才能上传超过100M的文件",null);
        }

//        String filename = CatUUID.randomUUID();
        String realFilename = "/头像/"+ Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId();
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
    public DTO<?> uploadAvatar(MultipartFile uploadFile, String userId) throws IOException {

//        String filename = CatUUID.randomUUID();

        String realFilename = "/头像/"+userId;
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
    public DTO<FileInfo> upload(MultipartFile uploadFile, String parentId, String realPath, String userId) throws IOException {
        long size = uploadFile.getSize();
        if(!SecurityUtils.isAdmin() && ( size >  100 * 1000 * 1000) ){
            return DTO.error("只有尊贵的VIP才能上传超过100M的文件，当然了没有成为VIP的方法",null);
        }

        FileInfo fileInfo = new FileInfo()
                .setId(CatUUID.randomUUID())
                .setFilename(uploadFile.getOriginalFilename())
                .setType(CONSTANTS.FILE_TYPE_1)
                .setParentId(parentId)
                .setSize(uploadFile.getSize())
                .setContentType(uploadFile.getContentType())
                .setUserId(Integer.parseInt(userId));
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
        download(fileId, UPLOAD_PATH+userMapper.selectById(fileInfoMapper.selectById(fileId).getUserId()).getIdStr()+"/");
    }

    @Override
    public void downloadAvatar(String userId) throws IOException {
        HttpServletResponse response = ServletUtils.getHttpServletResponse();
        HttpServletRequest request = ServletUtils.getHttpServletRequest();
        InputStream inputStream = null;
        try {
            // 设置响应头
            response.setContentType("application/octet-stream");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + URLEncoder.encode(userId, StandardCharsets.UTF_8) + "\"");
            // 获取 Range 头部信息
            String rangeHeader = request.getHeader(HttpHeaders.RANGE);
            // 如果没有 Range 头部，则直接返回整个文件内容
            inputStream = minioService.getObject(BUCKET_NAME, "/头像/"+userId);
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


    @Override
    public DTO<FileInfo> uploadDynamicForm(MultipartFile uploadFile) throws IOException {
        String userId = Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId();
        return upload(uploadFile, DYNAMIC_FORM,DYNAMIC_FORM, userId);
    }

    @Override
    public void downloadDynamicForm(String fileId) throws IOException {
        download(fileId, DYNAMIC_FORM);
    }

    @Override
    public DTO<FileInfo> uploadAgentFile(MultipartFile file) throws IOException {
        String userId = Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId();
        return upload(file, AGENT_FILE, AGENT_FILE, userId);
    }

    @Override
    public void downloadAgentFile(String fileId) throws IOException {
        download(fileId, AGENT_FILE);
    }

    @Override
    public String getAgentFileBase64(String fileId) {
        FileInfo fileInfo = fileInfoMapper.selectOne(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getId, fileId));
        return minioService.getBase64(BUCKET_NAME, AGENT_FILE+fileInfo.getId());
    }

    @Override
    public String getAgentFileBase64WithoutMineType(String fileId) {
        FileInfo fileInfo = fileInfoMapper.selectOne(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getId, fileId));
        return minioService.getBase64WithoutMimeType(BUCKET_NAME, AGENT_FILE+fileInfo.getId());
    }

    @Override
    public FileInfo getAgentFileInfoById(String fileId) {
        return fileInfoMapper.selectOne(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getParentId, AGENT_FILE).eq(FileInfo::getId, fileId));
    }

    @Override
    public String saveLocalAgentFileById(String fileId){
        FileInfo agentFileInfoById = getAgentFileInfoById(fileId);

        String path = LOCAL_FILE_TMP + UUIDUtils.randomUUID() + "/" + agentFileInfoById.getFilename();

        // 使用 try-with-resources 确保 InputStream 自动关闭
        try (InputStream inputStream = minioService.getObject(BUCKET_NAME, AGENT_FILE + fileId)) {

            Path destinationPath = Paths.get(path);

            // 可选：如果需要确保文件的父目录存在，可以加上以下代码
            File parentDir = destinationPath.getParent().toFile();
            if (!parentDir.exists()) {
                boolean mkdir = parentDir.mkdirs();
                if(!mkdir){
                    throw new IllegalStateException("文件夹创建失败");
                }
            }

            // 将 InputStream 复制到本地文件
            // StandardCopyOption.REPLACE_EXISTING 表示如果文件已存在则覆盖
            Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("文件已成功保存到: " + path);

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException("保存文件到本地失败", e);
        }
        return path;
    }

    private boolean notExistFolder(String folderId){
        Long parentFolderCount = fileInfoMapper.selectCount(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getUserId, SecurityUtils.getLoginUser().getUserId()).eq(FileInfo::getId, folderId).eq(FileInfo::getType,CONSTANTS.FILE_TYPE_2));
        return parentFolderCount != 1;
    }

}