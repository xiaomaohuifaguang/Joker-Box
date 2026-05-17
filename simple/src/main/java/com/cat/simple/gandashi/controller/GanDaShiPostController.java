package com.cat.simple.gandashi.controller;

import com.cat.common.entity.*;
import com.cat.common.entity.ganDaShi.GanDaShiPost;
import com.cat.common.entity.ganDaShi.GanDaShiPostPageParam;
import com.cat.simple.gandashi.service.GanDaShiPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ganDaShiPost")
@Tag(name = "干大事论坛")
public class GanDaShiPostController {

@Resource
private GanDaShiPostService ganDaShiPostService;

    @Operation(summary = "添加")
    @RequestMapping(value = "/add",method = RequestMethod.POST)
    public HttpResult<?> add(@RequestBody GanDaShiPost ganDaShiPost) {
        return HttpResult.back(ganDaShiPostService.add(ganDaShiPost) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "删除")
    @RequestMapping(value = "/remove",method = RequestMethod.POST)
    public HttpResult<?> remove(@RequestBody GanDaShiPost ganDaShiPost) {
        return HttpResult.back(ganDaShiPostService.delete(ganDaShiPost) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "详情")
    @RequestMapping(value = "/info",method = RequestMethod.POST)
    public HttpResult<GanDaShiPost> info(@RequestBody GanDaShiPost ganDaShiPost) {
        return HttpResult.back(ganDaShiPostService.info(ganDaShiPost));
    }

    @Operation(summary = "分页")
    @RequestMapping(value = "/queryPage",method = RequestMethod.POST)
    public HttpResult<Page<GanDaShiPost>> queryPage(@RequestBody GanDaShiPostPageParam pageParam) {
        return HttpResult.back(ganDaShiPostService.queryPage(pageParam));
    }



}
