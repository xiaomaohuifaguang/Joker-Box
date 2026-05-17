package com.cat.simple.info.controller;

import com.cat.common.entity.HttpResult;
import com.cat.common.entity.system.SystemPrompt;
import com.cat.simple.ai.service.SystemPromptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/system")
@Tag(name = "系统")
public class SystemController {

    @Resource
    private SystemPromptService promptService;


    @Operation(summary = "系统提示")
    @RequestMapping(value = "/prompt",method = RequestMethod.POST)
    public HttpResult<List<SystemPrompt>> prompt() {
        return HttpResult.back(promptService.queryAll());
    }




}
