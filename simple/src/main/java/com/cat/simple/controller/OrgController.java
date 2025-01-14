package com.cat.simple.controller;

import com.cat.common.entity.*;
import com.cat.common.entity.auth.Org;
import com.cat.common.entity.auth.OrgPageParam;
import com.cat.common.entity.auth.OrgTree;
import com.cat.simple.service.OrgService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/org")
@Tag(name = "机构管理")
public class OrgController {

    @Resource
    private OrgService orgService;

    @Operation(summary = "添加")
    @RequestMapping(value = "/add",method = RequestMethod.POST)
    public HttpResult<?> add(@RequestBody Org org) {
        return HttpResult.back(orgService.add(org) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "删除")
    @RequestMapping(value = "/remove",method = RequestMethod.POST)
    public HttpResult<?> remove(@RequestBody Org org) {
        return HttpResult.back(orgService.delete(org) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "修改")
    @RequestMapping(value = "/update",method = RequestMethod.POST)
    public HttpResult<?> update(@RequestBody Org org) {
        return HttpResult.back(orgService.update(org) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "详情")
    @RequestMapping(value = "/info",method = RequestMethod.POST)
    public HttpResult<Org> info(@RequestBody Org org) {
        return HttpResult.back(orgService.info(org));
    }

    @Operation(summary = "分页")
    @RequestMapping(value = "/queryPage",method = RequestMethod.POST)
    public HttpResult<Page<Org>> queryPage(@RequestBody OrgPageParam pageParam) {
        return HttpResult.back(orgService.queryPage(pageParam));
    }


    @Operation(summary = "组织机构树")
    @RequestMapping(value = "/getOrgTree",method = RequestMethod.POST)
    public HttpResult<OrgTree> getOrgTree() {
        return HttpResult.back(orgService.getOrgTree());
    }



}