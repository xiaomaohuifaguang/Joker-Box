package com.cat.simple.controller;

import com.cat.simple.service.ApiPathService;
import com.cat.common.entity.HttpResult;
import com.cat.common.entity.HttpResultStatus;
import com.cat.common.entity.Page;
import com.cat.common.entity.SelectOption;
import com.cat.common.entity.auth.ApiPath;
import com.cat.common.entity.auth.ApiPathPageParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/***
 * <TODO description class purpose>
 * @title AuthController
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/24 22:58
 **/
@RestController
@RequestMapping("/apiPath")
@Tag(name = "api路径管理")
public class ApiPathController {

    @Resource
    private ApiPathService apiPathService;


    @Operation(summary = "全量保存api路径")
    @Parameters({
            @Parameter(name = "server", description = "服务名称:application.name", required = true)
    })
    @RequestMapping(value = "/saveBatch", method = RequestMethod.POST)
    public HttpResult<?> saveBatch(@RequestParam("server") String server, @RequestBody List<ApiPath> apiPaths) {
        boolean flag = apiPathService.saveBatch(server, apiPaths);
        return HttpResult.back(flag ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "api列表")
    @RequestMapping(value = "/queryPage", method = RequestMethod.POST)
    public HttpResult<Page<ApiPath>> queryPage(@RequestBody ApiPathPageParam pageParam) {
        return HttpResult.back(apiPathService.queryPage(pageParam));
    }

    @Operation(summary = "api选择器")
    @Parameters({
            @Parameter(name = "server", description = "服务名称", required = false)
    })
    @RequestMapping(value = "/selector", method = RequestMethod.POST)
    public HttpResult<List<SelectOption>> selector(@RequestParam(value = "server", required = false) String server) {
        return HttpResult.back(apiPathService.selector(server));
    }


}
