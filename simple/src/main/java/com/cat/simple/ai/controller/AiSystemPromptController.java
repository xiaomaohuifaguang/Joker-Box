package com.cat.simple.ai.controller;

import com.cat.common.entity.HttpResult;
import com.cat.common.entity.HttpResultStatus;
import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.ai.systemPrompt.AiSystemPrompt;
import com.cat.simple.ai.service.AiSystemPromptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai/systemPrompt")
@Tag(name = "系统级提示词管理")
public class AiSystemPromptController {

    @Resource
    private AiSystemPromptService aiSystemPromptService;

    @Operation(summary = "添加")
    @RequestMapping(value = "/add",method = RequestMethod.POST)
    public HttpResult<?> add(@RequestBody AiSystemPrompt aiSystemPrompt){
        return HttpResult.back(aiSystemPromptService.add(aiSystemPrompt) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "删除")
    @RequestMapping(value = "/delete",method = RequestMethod.POST)
    @Parameters({
            @Parameter(name = "id",description = "id",required = true)
    })
    public HttpResult<?> delete(@RequestParam("id") Integer id){
        return HttpResult.back(aiSystemPromptService.delete(id) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }


    @Operation(summary = "修改")
    @RequestMapping(value = "/update",method = RequestMethod.POST)
    public HttpResult<?> update(@RequestBody AiSystemPrompt aiSystemPrompt){
        return HttpResult.back(aiSystemPromptService.update(aiSystemPrompt) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }


    @Operation(summary = "详情")
    @RequestMapping(value = "/info",method = RequestMethod.POST)
    @Parameters({
            @Parameter(name = "id",description = "id",required = true)
    })
    public HttpResult<AiSystemPrompt> info(@RequestParam("id") Integer id){
        return HttpResult.back(aiSystemPromptService.info(id) );
    }


    @Operation(summary = "分页查询")
    @RequestMapping(value = "/queryPage",method = RequestMethod.POST)
    public HttpResult<Page<AiSystemPrompt>> queryPage(@RequestBody PageParam param){
        return HttpResult.back(aiSystemPromptService.queryPage(param));
    }


}
