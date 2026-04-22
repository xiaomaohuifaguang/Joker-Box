package com.cat.simple.controller.ai;

import com.cat.common.entity.*;
import com.cat.common.entity.ai.model.AiModel;
import com.cat.simple.service.AiModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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
    public HttpResult<Page<AiModel>> queryPage(@RequestBody PageParam pageParam) {
        return HttpResult.back(aiModelService.queryPage(pageParam));
    }



}
