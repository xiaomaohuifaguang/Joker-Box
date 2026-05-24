package com.cat.simple.codeTable.controller;

import com.cat.common.entity.HttpResult;
import com.cat.common.entity.HttpResultStatus;
import com.cat.common.entity.codeTable.CodeItem;
import com.cat.common.entity.codeTable.CodeItemQueryParam;
import com.cat.common.entity.codeTable.CodeOption;
import com.cat.simple.codeTable.service.CodeTableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/code-item")
@Tag(name = "码表项管理")
public class CodeItemController {

    @Resource
    private CodeTableService codeTableService;

    @Operation(summary = "码表项列表")
    @PostMapping("/list")
    public HttpResult<List<CodeItem>> list(@RequestBody CodeItemQueryParam param) {
        return HttpResult.back(codeTableService.listItems(param));
    }

    @Operation(summary = "新增码表项")
    @PostMapping("/add")
    public HttpResult<CodeItem> add(@RequestBody CodeItem codeItem) {
        return HttpResult.back(codeTableService.addItem(codeItem));
    }

    @Operation(summary = "更新码表项")
    @PostMapping("/update")
    public HttpResult<CodeItem> update(@RequestBody CodeItem codeItem) {
        return HttpResult.back(codeTableService.updateItem(codeItem));
    }

    @Operation(summary = "删除码表项")
    @Parameters({
            @Parameter(name = "id", description = "码表项id", required = true)
    })
    @PostMapping("/delete")
    public HttpResult<?> delete(@RequestParam("id") String id) {
        codeTableService.deleteItem(id);
        return HttpResult.back(HttpResultStatus.SUCCESS);
    }

    @Operation(summary = "码表项树")
    @PostMapping("/tree")
    public HttpResult<List<CodeOption>> tree(@RequestBody CodeItemQueryParam param) {
        return HttpResult.back(codeTableService.treeItems(param));
    }
}
