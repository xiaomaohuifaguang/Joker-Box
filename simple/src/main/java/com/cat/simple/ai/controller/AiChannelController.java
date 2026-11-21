package com.cat.simple.ai.controller;

import com.cat.common.entity.*;
import com.cat.common.entity.ai.model.AiModel;
import com.cat.simple.ai.service.AiChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.hc.core5.http.ParseException;
import org.springframework.web.bind.annotation.*;
import com.cat.common.entity.ai.channel.AiChannel;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/ai/channel")
@Tag(name = "ai模型渠道")
public class AiChannelController {

    @Resource
    private AiChannelService aiChannelService;

    @Operation(summary = "添加")
    @RequestMapping(value = "/add",method = RequestMethod.POST)
    public HttpResult<?> add(@RequestBody AiChannel aiChannel) {
        return HttpResult.back(aiChannelService.add(aiChannel) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "删除")
    @RequestMapping(value = "/remove",method = RequestMethod.POST)
    public HttpResult<?> remove(@RequestBody AiChannel aiChannel) {
        return HttpResult.back(aiChannelService.delete(aiChannel) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "修改")
    @RequestMapping(value = "/update",method = RequestMethod.POST)
    public HttpResult<?> update(@RequestBody AiChannel aiChannel) {
        return HttpResult.back(aiChannelService.update(aiChannel) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "详情")
    @RequestMapping(value = "/info",method = RequestMethod.POST)
    public HttpResult<AiChannel> info(@RequestBody AiChannel aiChannel) {
        return HttpResult.back(aiChannelService.info(aiChannel));
    }

    @Operation(summary = "分页")
    @RequestMapping(value = "/queryPage",method = RequestMethod.POST)
    public HttpResult<Page<AiChannel>> queryPage(@RequestBody PageParam pageParam) {
        return HttpResult.back(aiChannelService.queryPage(pageParam));
    }

    @Operation(summary = "测试")
    @RequestMapping(value = "/test",method = RequestMethod.POST)
    public HttpResult<Boolean> test(@RequestBody AiChannel aiChannel) throws IOException, ParseException {
        return HttpResult.back(aiChannelService.test(aiChannel));
    }


    @Operation(summary = "模型列表")
    @RequestMapping(value = "/models",method = RequestMethod.POST)
    public HttpResult<List<AiModel>> models(@RequestBody AiChannel aiChannel) throws IOException, ParseException {
        return HttpResult.back(aiChannelService.models(aiChannel));
    }



}
