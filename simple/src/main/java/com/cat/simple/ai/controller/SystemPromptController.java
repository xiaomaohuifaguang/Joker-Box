package com.cat.simple.ai.controller;

import com.cat.common.entity.*;
import com.cat.common.entity.system.SystemPrompt;
import com.cat.simple.ai.service.SystemPromptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/systemPrompt")
@Tag(name = "系统提示")
public class SystemPromptController {

@Resource
private SystemPromptService systemPromptService;

    @Operation(summary = "添加")
    @RequestMapping(value = "/add",method = RequestMethod.POST)
    public HttpResult<?> add(@RequestBody SystemPrompt systemPrompt) {
        return HttpResult.back(systemPromptService.add(systemPrompt) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "删除")
    @RequestMapping(value = "/remove",method = RequestMethod.POST)
    public HttpResult<?> remove(@RequestBody SystemPrompt systemPrompt) {
        return HttpResult.back(systemPromptService.delete(systemPrompt) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "详情")
    @RequestMapping(value = "/info",method = RequestMethod.POST)
    public HttpResult<SystemPrompt> info(@RequestBody SystemPrompt systemPrompt) {
        return HttpResult.back(systemPromptService.info(systemPrompt));
    }

    @Operation(summary = "分页")
    @RequestMapping(value = "/queryPage",method = RequestMethod.POST)
    public HttpResult<Page<SystemPrompt>> queryPage(@RequestBody PageParam pageParam) {
        return HttpResult.back(systemPromptService.queryPage(pageParam));
    }



}
