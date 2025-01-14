package com.cat.simple.controller;

import com.cat.common.entity.*;
import com.cat.simple.service.WebLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webLog")
@Tag(name = "webLog")
public class WebLogController {

@Resource
private WebLogService webLogService;

    @Operation(summary = "添加")
    @RequestMapping(value = "/add",method = RequestMethod.POST)
    public HttpResult<?> add(@RequestBody WebLog webLog) {
        return HttpResult.back(webLogService.add(webLog) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "删除")
    @RequestMapping(value = "/remove",method = RequestMethod.POST)
    public HttpResult<?> remove(@RequestBody WebLog webLog) {
        return HttpResult.back(webLogService.delete(webLog) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "修改")
    @RequestMapping(value = "/update",method = RequestMethod.POST)
    public HttpResult<?> update(@RequestBody WebLog webLog) {
        return HttpResult.back(webLogService.update(webLog) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "详情")
    @RequestMapping(value = "/info",method = RequestMethod.POST)
    public HttpResult<WebLog> info(@RequestBody WebLog webLog) {
        return HttpResult.back(webLogService.info(webLog));
    }

    @Operation(summary = "分页")
    @RequestMapping(value = "/queryPage",method = RequestMethod.POST)
    public HttpResult<Page<WebLog>> queryPage(@RequestBody PageParam pageParam) {
        return HttpResult.back(webLogService.queryPage(pageParam));
    }



}