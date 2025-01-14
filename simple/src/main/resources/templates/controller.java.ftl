package com.cat.simple.controller;

import com.cat.common.entity.*;
import com.cat.simple.service.${tableNameUp}Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/${tableNameDown}")
@Tag(name = "${tableNameDown}")
public class ${tableNameUp}Controller {

@Resource
private ${tableNameUp}Service ${tableNameDown}Service;

    @Operation(summary = "添加")
    @RequestMapping(value = "/add",method = RequestMethod.POST)
    public HttpResult<?> add(@RequestBody ${tableNameUp} ${tableNameDown}) {
        return HttpResult.back(${tableNameDown}Service.add(${tableNameDown}) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "删除")
    @RequestMapping(value = "/remove",method = RequestMethod.POST)
    public HttpResult<?> remove(@RequestBody ${tableNameUp} ${tableNameDown}) {
        return HttpResult.back(${tableNameDown}Service.delete(${tableNameDown}) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "修改")
    @RequestMapping(value = "/update",method = RequestMethod.POST)
    public HttpResult<?> update(@RequestBody ${tableNameUp} ${tableNameDown}) {
        return HttpResult.back(${tableNameDown}Service.update(${tableNameDown}) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "详情")
    @RequestMapping(value = "/info",method = RequestMethod.POST)
    public HttpResult<${tableNameUp}> info(@RequestBody ${tableNameUp} ${tableNameDown}) {
        return HttpResult.back(${tableNameDown}Service.info(${tableNameDown}));
    }

    @Operation(summary = "分页")
    @RequestMapping(value = "/queryPage",method = RequestMethod.POST)
    public HttpResult<Page<${tableNameUp}>> queryPage(@RequestBody PageParam pageParam) {
        return HttpResult.back(${tableNameDown}Service.queryPage(pageParam));
    }



}
