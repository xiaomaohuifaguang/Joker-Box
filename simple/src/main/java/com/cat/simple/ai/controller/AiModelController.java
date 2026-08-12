package com.cat.simple.ai.controller;

import com.cat.common.entity.*;
import com.cat.common.entity.ai.model.AiModel;
import com.cat.common.entity.ai.model.AiModelPageParam;
import com.cat.simple.ai.service.AiModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ai/model")
@Tag(name = "模型管理")
public class AiModelController {

@Resource
private AiModelService aiModelService;

    @Operation(summary = "添加")
    @RequestMapping(value = "/add",method = RequestMethod.POST)
    public HttpResult<?> add(@RequestBody AiModel aiModel) {
        return HttpResult.back(aiModelService.add(aiModel) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "删除")
    @RequestMapping(value = "/remove",method = RequestMethod.POST)
    public HttpResult<?> remove(@RequestBody AiModel aiModel) {
        return HttpResult.back(aiModelService.delete(aiModel) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "修改")
    @RequestMapping(value = "/update",method = RequestMethod.POST)
    public HttpResult<?> update(@RequestBody AiModel aiModel) {
        return HttpResult.back(aiModelService.update(aiModel) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "详情")
    @RequestMapping(value = "/info",method = RequestMethod.POST)
    public HttpResult<AiModel> info(@RequestBody AiModel aiModel) {
        return HttpResult.back(aiModelService.info(aiModel));
    }

    @Operation(summary = "分页")
    @RequestMapping(value = "/queryPage",method = RequestMethod.POST)
    public HttpResult<Page<AiModel>> queryPage(@RequestBody AiModelPageParam pageParam) {
        return HttpResult.back(aiModelService.queryPage(pageParam));
    }

    @Operation(summary = "默认模型配置信息")
    @RequestMapping(value = "/defaultModelSettings",method = RequestMethod.POST)
    public HttpResult<Map<String, AiModel>> defaultModelSettings(){
        return HttpResult.back(aiModelService.defaultModel());
    }

    @Operation(summary = "设置默认模型")
    @Parameters({
            @Parameter(name = "type", description = "类型",required = true),
            @Parameter(name = "modelId", description = "模型id",required = true),
    })
    @RequestMapping(value = "/setDefaultModel",method = RequestMethod.POST)
    public HttpResult<?> defaultModelSettings(@RequestParam("type") String type,@RequestParam("modelId") String modelId){
        return HttpResult.back(aiModelService.setDefaultModel(type, modelId));
    }

    @Operation(summary = "解绑默认模型配置")
    @Parameters({
            @Parameter(name = "type", description = "类型",required = true)
    })
    @RequestMapping(value = "/clearDefaultModel",method = RequestMethod.POST)
    public HttpResult<?> clearDefaultModel(@RequestParam("type") String type){
        return HttpResult.back(aiModelService.clearDefaultModel(type));
    }


}
