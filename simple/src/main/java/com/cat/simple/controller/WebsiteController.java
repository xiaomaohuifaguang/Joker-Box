package com.cat.simple.controller;

import com.cat.common.entity.HttpResult;
import com.cat.common.entity.HttpResultStatus;
import com.cat.common.entity.Page;
import com.cat.common.entity.website.Website;
import com.cat.common.entity.website.WebsiteGroup;
import com.cat.common.entity.website.WebsitePageParam;
import com.cat.simple.service.WebsiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/***
 * <TODO description class purpose>
 * @title WebsiteController
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/10/17
 **/
@RestController
@RequestMapping("/website")
@Tag(name = "网站收藏")
public class WebsiteController {

    @Resource
    private WebsiteService websiteService;

    @Operation(summary = "网站列表(分页)")
    @RequestMapping(value = "/queryPage",method = RequestMethod.POST)
    public HttpResult<Page<Website>> queryPage(@RequestBody WebsitePageParam pageParam){
        Page<Website> websitePage = websiteService.queryPage(pageParam);
        return HttpResult.back(websitePage);
    }

    @Operation(summary = "网站列表(分组)")
    @RequestMapping(value = "/group",method = RequestMethod.POST)
    public HttpResult<List<WebsiteGroup>> group(){
        return HttpResult.back( websiteService.groups());
    }

    @Operation(summary = "添加收藏")
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public HttpResult<?> add(@RequestBody Website website){
        boolean add = websiteService.add(website);
        return HttpResult.back(add ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "删除收藏")
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    @Parameters({
            @Parameter(name = "id", description = "id", required = true)
    })
    public HttpResult<?> delete(@RequestParam("id") Integer id){
        boolean delete = websiteService.delete(id);
        return HttpResult.back(delete ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "修改")
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public HttpResult<?> save(@RequestBody Website website){
        boolean update = websiteService.update(website);
        return HttpResult.back(update ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "网站信息")
    @Parameters({
            @Parameter(name = "id", description = "id", required = true)
    })
    @RequestMapping(value = "/info", method = RequestMethod.POST)
    public HttpResult<Website> info(@RequestParam("id") Integer id){
        return HttpResult.back(websiteService.info(id));
    }



}
