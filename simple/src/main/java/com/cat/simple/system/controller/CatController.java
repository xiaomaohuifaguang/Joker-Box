package com.cat.simple.system.controller;

import com.cat.common.entity.HttpResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "系统级接口")
public class CatController {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${custom.info.version}")
    private String version;

    @Operation(summary = "在活检查")
    @GetMapping("/alive")
    public HttpResult<?>  alive(){
        return HttpResult.back(applicationName + ":" + version);
    }

    @Operation(summary = "版本")
    @GetMapping("/version")
    public HttpResult<?>  version(){
        return HttpResult.back(version);
    }


}
