package com.cat.simple.ai.tools.file;

import com.cat.common.entity.LocalMultipartFile;
import com.cat.common.entity.minerU.ParseRequest;
import com.cat.common.entity.minerU.ParseResponse;
import com.cat.common.utils.IOUtils;
import com.cat.simple.file.service.FileService;
import com.cat.simple.remote.mineru.MinerUClient;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Component
public class FileParseTools {

    @Resource
    private MinerUClient minerUClient;

    @Resource
    private FileService fileService;


    @Tool(description = "通过文件id将文件转markdown格式 注意仅支持文档类型 ")
    public String parseToMarkdown(@ToolParam(required = true, description = "文件id") String fileId, ToolContext toolContext) throws IOException {

        String savePath = fileService.saveLocalAgentFileById(fileId);

        File file = new File(savePath);
        String contentType = Files.probeContentType(file.toPath());
        // 如果探测不到（某些系统可能返回 null），给个默认值
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE; // "application/octet-stream"
        }

        LocalMultipartFile files = new LocalMultipartFile(file, "files", contentType);

        ParseRequest parseRequest = new ParseRequest();
        parseRequest.setBackend("pipeline");
        parseRequest.setFiles(new MultipartFile[]{files});
        ParseResponse parseResponse = minerUClient.fileParse(parseRequest);
        boolean delete = file.delete();
        return parseResponse.getResults().get(IOUtils.fileNameWithoutType(file.getName())).getMd_content();
    }


}
