package com.cat.api.file;

import com.cat.common.entity.FileInfo;
import com.cat.common.entity.HttpResult;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;


/***
 * <TODO description class purpose>
 * @title FileServiceClient
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/27 21:11
 **/
@FeignClient(value = "file-server", path = "/file", contextId = "FileServiceClient")
@Component
public interface FileServiceClient {

    @RequestMapping(value = "/file/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, method = RequestMethod.POST)
    HttpResult<FileInfo> upload(@RequestPart(value = "uploadFile") MultipartFile uploadFile,@RequestPart("parentId") String parentId);

    @RequestMapping(value = "/file/createFolder", method = RequestMethod.POST)
    HttpResult<FileInfo> createFolder(@RequestParam(value = "fileName") String fileName,@RequestParam("parentId") String parentId);

    @RequestMapping(value = "/file/download", method = RequestMethod.GET)
    Response download(@RequestParam(value = "fileId") String fileId);

}
