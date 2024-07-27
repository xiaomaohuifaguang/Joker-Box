package com.cat.file.controller;

import com.cat.common.entity.FileInfo;
import com.cat.common.entity.HttpResult;
import com.cat.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

/***
 * 文件服务控制层
 * @title FileController
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/26 23:24
 **/
@RestController
@RequestMapping("/file")
@Tag(name = "文件服务")
public class FileController {

    @Resource
    private FileService fileService;

    @Operation(summary = "上传文件")
    @Parameters({
            @Parameter(name = "uploadFile", schema = @Schema(format = "binary"), description = "文件",required = true),
            @Parameter(name = "parentId",description = "父级文件加id",required = true)
    })
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public HttpResult<FileInfo> upload(@RequestPart(value = "uploadFile") MultipartFile uploadFile, @RequestPart("parentId") String parentId) throws IOException {
        return HttpResult.back(fileService.upload(uploadFile,parentId));
    }

    @Operation(summary = "创建文件夹")
    @Parameters({
            @Parameter(name = "fileName",description = "文件夹名称",required = true),
            @Parameter(name = "parentId",description = "父级文件夹id",required = true)
    })
    @RequestMapping(value = "/createFolder", method = RequestMethod.POST)
    public HttpResult<FileInfo> createFolder(@RequestParam(value = "fileName") String fileName, @RequestParam("parentId") String parentId) throws IOException {
        return HttpResult.back(fileService.createFolder(fileName,parentId));
    }

    @Operation(summary = "下载文件")
    @Parameters({
            @Parameter(name = "fileId", description = "文件唯一id", required = true)
    })
    @RequestMapping(value = "/download", method = RequestMethod.GET)
    public void download(@RequestParam(value = "fileId") String fileId) throws IOException {
        fileService.download(fileId);
    }

    @Operation(summary = "文件列表")
    @Parameters({
            @Parameter(name = "parentId",description = "父级文件夹id",required = true),
    })
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public HttpResult<List<FileInfo>> list(@RequestParam("parentId") String parentId) throws IOException {
        return HttpResult.back(fileService.list(parentId));
    }

    @Operation(summary = "文件/文件夹删除")
    @Parameters({
            @Parameter(name = "fileId",description = "文件id",required = true),
    })
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public HttpResult<?> delete(@RequestParam("fileId") String fileId) throws IOException {
        return HttpResult.back(fileService.delete(fileId));
    }

    @Operation(summary = "文件/文件夹重命名")
    @Parameters({
            @Parameter(name = "fileId",description = "文件id",required = true),
            @Parameter(name = "filename",description = "文件名称",required = true),
    })
    @RequestMapping(value = "/rename", method = RequestMethod.POST)
    public HttpResult<?> rename(@RequestParam("fileId") String fileId, @RequestParam("filename") String filename) throws IOException {
        return HttpResult.back(fileService.rename(fileId,filename));
    }

}
