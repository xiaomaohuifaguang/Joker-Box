package com.cat.simple.controller;

import com.cat.common.entity.*;
import com.cat.common.entity.crawler.CrawlerTask;
import com.cat.simple.service.CrawlerTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/crawlerTask")
@Tag(name = "爬虫任务")
public class CrawlerTaskController {

@Resource
private CrawlerTaskService crawlerTaskService;

    @Operation(summary = "添加")
    @RequestMapping(value = "/add",method = RequestMethod.POST)
    public HttpResult<?> add(@RequestBody CrawlerTask crawlerTask) {
        return HttpResult.back(crawlerTaskService.add(crawlerTask) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "删除")
    @RequestMapping(value = "/remove",method = RequestMethod.POST)
    public HttpResult<?> remove(@RequestBody CrawlerTask crawlerTask) {
        return HttpResult.back(crawlerTaskService.delete(crawlerTask) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "修改")
    @RequestMapping(value = "/update",method = RequestMethod.POST)
    public HttpResult<?> update(@RequestBody CrawlerTask crawlerTask) {
        return HttpResult.back(crawlerTaskService.update(crawlerTask) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "详情")
    @RequestMapping(value = "/info",method = RequestMethod.POST)
    public HttpResult<CrawlerTask> info(@RequestBody CrawlerTask crawlerTask) {
        return HttpResult.back(crawlerTaskService.info(crawlerTask));
    }

    @Operation(summary = "分页")
    @RequestMapping(value = "/queryPage",method = RequestMethod.POST)
    public HttpResult<Page<CrawlerTask>> queryPage(@RequestBody PageParam pageParam) {
        return HttpResult.back(crawlerTaskService.queryPage(pageParam));
    }



}
