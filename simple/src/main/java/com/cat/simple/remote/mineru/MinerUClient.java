package com.cat.simple.remote.mineru;

import com.cat.common.entity.minerU.ParseRequest;
import com.cat.common.entity.minerU.ParseResponse;
import com.cat.common.entity.minerU.TaskResponse;
import com.cat.common.entity.minerU.TaskResultResponse;
import com.cat.simple.config.feign.MinerUFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "mineru-client", url = "${custom.feign.minerU.url:http://127.0.0.1:8000/}", configuration = MinerUFeignConfig.class)
public interface MinerUClient {


//    @PostMapping(value = "/file_parse",  consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    Object fileParse(// 1. 文件参数：使用 @RequestPart
//                     @RequestPart("files") MultipartFile[] files,
//                     @RequestPart("backend") String backend
//    );


    @PostMapping(value = "/file_parse",  consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ParseResponse fileParse(// 1. 文件参数：使用 @RequestPart
                            @RequestBody ParseRequest parseRequest
    );


    @PostMapping(value = "/tasks",  consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    TaskResponse tasks(// 1. 文件参数：使用 @RequestPart
                       @RequestBody ParseRequest parseRequest
    );

    @GetMapping(value = "/tasks/{taskId}")
    TaskResponse taskStatus(@PathVariable String taskId);


    @GetMapping(value = "/tasks/{taskId}/result")
    TaskResultResponse taskResult(@PathVariable String taskId);


}
