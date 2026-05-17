package com.cat.simple.gandashi.controller;

import com.cat.common.entity.*;
import com.cat.common.entity.ganDaShi.GanDaShiComment;
import com.cat.common.entity.ganDaShi.GanDaShiCommentPageParam;
import com.cat.simple.gandashi.service.GanDaShiCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/ganDaShiComment")
@Tag(name = "干大事论坛评论")
public class GanDaShiCommentController {

@Resource
    private GanDaShiCommentService ganDaShiCommentService;

    @Operation(summary = "添加")
    @RequestMapping(value = "/add",method = RequestMethod.POST)
    public HttpResult<?> add(@RequestBody GanDaShiComment ganDaShiComment) {
        GanDaShiComment add = ganDaShiCommentService.add(ganDaShiComment);
        return HttpResult.back(Objects.nonNull(add) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR, add);
    }

    @Operation(summary = "删除")
    @RequestMapping(value = "/remove",method = RequestMethod.POST)
    public HttpResult<?> remove(@RequestBody GanDaShiComment ganDaShiComment) {
        return HttpResult.back(ganDaShiCommentService.delete(ganDaShiComment) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }



    @Operation(summary = "详情")
    @RequestMapping(value = "/info",method = RequestMethod.POST)
    public HttpResult<GanDaShiComment> info(@RequestBody GanDaShiComment ganDaShiComment) {
        return HttpResult.back(ganDaShiCommentService.info(ganDaShiComment));
    }

    @Operation(summary = "分页")
    @RequestMapping(value = "/queryPage",method = RequestMethod.POST)
    public HttpResult<Page<GanDaShiComment>> queryPage(@RequestBody GanDaShiCommentPageParam pageParam) {
        return HttpResult.back(ganDaShiCommentService.queryPage(pageParam));
    }



}
