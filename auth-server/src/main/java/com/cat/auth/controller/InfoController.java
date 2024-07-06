package com.cat.auth.controller;

import com.cat.common.entity.HttpResult;
import com.cat.common.entity.HttpResultStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/***
 * <TODO description class purpose>
 * @title InfoController
 * @description 系统信息接口
 * @author xiaomaohuifaguang
 * @create 2024/6/19 22:34
 **/
@RestController
@RequestMapping("/info")
@Tag(name = "服务信息")
public class InfoController {

    @Value("${custom.info.version}")
    private String version;

    @Operation(summary = "版本")
    @RequestMapping(value = "/version", method = RequestMethod.GET)
    public HttpResult<String> version() {
        return HttpResult.back(HttpResultStatus.SUCCESS, version);
    }


}
