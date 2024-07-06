package com.cat.file.controller;

import com.cat.common.entity.FileInfo;
import com.cat.common.entity.HttpResult;
import com.cat.common.entity.HttpResultStatus;
import com.cat.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

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
            @Parameter(name = "uploadFile", schema = @Schema(format = "binary"), description = "文件",required = true)
    })
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public HttpResult<FileInfo> upload(@RequestPart(value = "uploadFile") MultipartFile uploadFile) throws IOException {
        FileInfo upload = fileService.upload(uploadFile);
        return ObjectUtils.isEmpty(upload) ? HttpResult.back(HttpResultStatus.ERROR) : HttpResult.back(upload);
    }

    @Operation(summary = "下载文件")
    @Parameters({
            @Parameter(name = "fileId", description = "文件唯一id", required = true)
    })
    @RequestMapping(value = "/download", method = RequestMethod.GET)
    public void download(@RequestParam(value = "fileId") String fileId) throws IOException {
        fileService.download(fileId);
    }

}
