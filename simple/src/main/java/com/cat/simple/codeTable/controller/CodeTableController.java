package com.cat.simple.codeTable.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.common.entity.HttpResult;
import com.cat.common.entity.HttpResultStatus;
import com.cat.common.entity.codeTable.CodeOption;
import com.cat.common.entity.codeTable.CodeTable;
import com.cat.common.entity.codeTable.CodeTablePageParam;
import com.cat.simple.codeTable.service.CodeTableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/code-table")
@Tag(name = "码表管理")
public class CodeTableController {

    @Resource
    private CodeTableService codeTableService;

    @Operation(summary = "码表分页")
    @PostMapping("/page")
    public HttpResult<Page<CodeTable>> page(@RequestBody CodeTablePageParam param) {
        return HttpResult.back(codeTableService.page(param));
    }

    @Operation(summary = "新增码表")
    @PostMapping("/add")
    public HttpResult<CodeTable> add(@RequestBody CodeTable codeTable) {
        return HttpResult.back(codeTableService.addTable(codeTable));
    }

    @Operation(summary = "更新码表")
    @PostMapping("/update")
    public HttpResult<CodeTable> update(@RequestBody CodeTable codeTable) {
        return HttpResult.back(codeTableService.updateTable(codeTable));
    }

    @Operation(summary = "删除码表")
    @Parameters({
            @Parameter(name = "id", description = "码表id", required = true)
    })
    @PostMapping("/delete")
    public HttpResult<?> delete(@RequestParam("id") String id) {
        codeTableService.deleteTable(id);
        return HttpResult.back(HttpResultStatus.SUCCESS);
    }

    @Operation(summary = "码表详情")
    @Parameters({
            @Parameter(name = "id", description = "码表id", required = true)
    })
    @PostMapping("/detail")
    public HttpResult<CodeTable> detail(@RequestParam("id") String id) {
        return HttpResult.back(codeTableService.detailTable(id));
    }

    @Operation(summary = "码表选项")
    @Parameters({
            @Parameter(name = "code", description = "码表编码", required = true)
    })
    @GetMapping("/options")
    public HttpResult<List<CodeOption>> options(@RequestParam("code") String code) {
        return HttpResult.back(codeTableService.options(code));
    }
}
