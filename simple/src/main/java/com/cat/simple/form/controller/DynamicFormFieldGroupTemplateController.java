package com.cat.simple.form.controller;

import com.cat.common.entity.*;
import com.cat.simple.form.service.DynamicFormFieldGroupTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.cat.common.entity.dynamicForm.DynamicFormFieldGroupTemplate;

@RestController
@RequestMapping("/dynamicFormFieldGroupTemplate")
@Tag(name = "字段组模板")
public class DynamicFormFieldGroupTemplateController {

    @Resource
    private DynamicFormFieldGroupTemplateService dynamicFormFieldGroupTemplateService;

    @Operation(summary = "添加")
    @RequestMapping(value = "/add",method = RequestMethod.POST)
    public HttpResult<?> add(@RequestBody DynamicFormFieldGroupTemplate dynamicFormFieldGroupTemplate) {
        return HttpResult.back(dynamicFormFieldGroupTemplateService.add(dynamicFormFieldGroupTemplate) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "删除")
    @RequestMapping(value = "/remove",method = RequestMethod.POST)
    public HttpResult<?> remove(@RequestBody DynamicFormFieldGroupTemplate dynamicFormFieldGroupTemplate) {
        return HttpResult.back(dynamicFormFieldGroupTemplateService.delete(dynamicFormFieldGroupTemplate) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "修改")
    @RequestMapping(value = "/update",method = RequestMethod.POST)
    public HttpResult<?> update(@RequestBody DynamicFormFieldGroupTemplate dynamicFormFieldGroupTemplate) {
        return HttpResult.back(dynamicFormFieldGroupTemplateService.update(dynamicFormFieldGroupTemplate) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "详情")
    @RequestMapping(value = "/info",method = RequestMethod.POST)
    public HttpResult<DynamicFormFieldGroupTemplate> info(@RequestBody DynamicFormFieldGroupTemplate dynamicFormFieldGroupTemplate) {
        return HttpResult.back(dynamicFormFieldGroupTemplateService.info(dynamicFormFieldGroupTemplate));
    }

    @Operation(summary = "分页")
    @RequestMapping(value = "/queryPage",method = RequestMethod.POST)
    public HttpResult<Page<DynamicFormFieldGroupTemplate>> queryPage(@RequestBody PageParam pageParam) {
        return HttpResult.back(dynamicFormFieldGroupTemplateService.queryPage(pageParam));
    }



}
