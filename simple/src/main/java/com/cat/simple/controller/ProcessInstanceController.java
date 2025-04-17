package com.cat.simple.controller;

import com.cat.common.entity.*;
import com.cat.common.entity.process.ProcessInfo;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.common.entity.process.ProcessInstancePageParam;
import com.cat.simple.service.ProcessInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/processInstance")
@Tag(name = "流程审批")
public class ProcessInstanceController {

    @Resource
    private ProcessInstanceService processInstanceService;

//    @Operation(summary = "分页")
//    @RequestMapping(value = "/queryPage",method = RequestMethod.POST)
//    public HttpResult<Page<ProcessInstance>> queryPage(@RequestBody ProcessInstancePageParam pageParam) {
//        return HttpResult.back(processInstanceService.queryPage(pageParam));
//    }
//
//    @Operation(summary = "启动")
//    @RequestMapping(value = "/start",method = RequestMethod.POST)
//    public HttpResult<ProcessInstance> start(@RequestParam Integer processDefinitionId) {
//        ProcessInstance start = processInstanceService.start(processDefinitionId);
//        return HttpResult.back(Objects.isNull(start) ? HttpResultStatus.ERROR : HttpResultStatus.SUCCESS);
//    }
//
//
//    @Operation(summary = "详情")
//    @RequestMapping(value = "/info",method = RequestMethod.POST)
//    public HttpResult<ProcessInfo> info(@RequestParam Integer processInstanceId) {
//        ProcessInfo info = processInstanceService.info(processInstanceId);
//        return HttpResult.back(Objects.isNull(info) ? HttpResultStatus.ERROR : HttpResultStatus.SUCCESS, info);
//    }
//
//    @Operation(summary = "处理信息")
//    @RequestMapping(value = "/handleInfo",method = RequestMethod.POST)
//    public HttpResult<ProcessInfo> handleInfo(@RequestParam Integer processInstanceId) {
//        ProcessInfo info = processInstanceService.handleInfo(processInstanceId);
//        return HttpResult.back(Objects.isNull(info) ? HttpResultStatus.ERROR : HttpResultStatus.SUCCESS, info);
//    }
//
//    @Operation(summary = "通过")
//    @RequestMapping(value = "/pass",method = RequestMethod.POST)
//    public HttpResult<?> pass(@RequestParam Integer processInstanceId) {
//        return HttpResult.back(processInstanceService.pass(processInstanceId) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
//    }
//
//
//    @Operation(summary = "转办")
//    @RequestMapping(value = "/transfer",method = RequestMethod.POST)
//    public HttpResult<?> transfer(@RequestParam Integer processInstanceId, @RequestParam Integer userId) {
//        return HttpResult.back(processInstanceService.transfer(processInstanceId, userId) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
//    }
//
//    @Operation(summary = "拒绝")
//    @RequestMapping(value = "/reject",method = RequestMethod.POST)
//    public HttpResult<?> reject(@RequestParam Integer processInstanceId) {
//        return HttpResult.back(processInstanceService.reject(processInstanceId) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
//    }








}
