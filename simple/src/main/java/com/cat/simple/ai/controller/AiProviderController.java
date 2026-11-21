package com.cat.simple.ai.controller;

import com.cat.common.entity.*;
import com.cat.simple.ai.service.AiProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.cat.common.entity.ai.provider.AiProvider;

@RestController
@RequestMapping("/ai/provider")
@Tag(name = "ai供应商")
public class AiProviderController {

    @Resource
    private AiProviderService aiProviderService;

    @Operation(summary = "添加")
    @RequestMapping(value = "/add",method = RequestMethod.POST)
    public HttpResult<?> add(@RequestBody AiProvider aiProvider) {
        return HttpResult.back(aiProviderService.add(aiProvider) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "删除")
    @RequestMapping(value = "/remove",method = RequestMethod.POST)
    public HttpResult<?> remove(@RequestBody AiProvider aiProvider) {
        return HttpResult.back(aiProviderService.delete(aiProvider) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "修改")
    @RequestMapping(value = "/update",method = RequestMethod.POST)
    public HttpResult<?> update(@RequestBody AiProvider aiProvider) {
        return HttpResult.back(aiProviderService.update(aiProvider) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "详情")
    @RequestMapping(value = "/info",method = RequestMethod.POST)
    public HttpResult<AiProvider> info(@RequestBody AiProvider aiProvider) {
        return HttpResult.back(aiProviderService.info(aiProvider));
    }

    @Operation(summary = "分页")
    @RequestMapping(value = "/queryPage",method = RequestMethod.POST)
    public HttpResult<Page<AiProvider>> queryPage(@RequestBody PageParam pageParam) {
        return HttpResult.back(aiProviderService.queryPage(pageParam));
    }



}
