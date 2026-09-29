package com.cat.simple.form.controller;

import com.cat.common.entity.*;
import com.cat.simple.form.service.DynamicFormFieldTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.cat.common.entity.dynamicForm.DynamicFormFieldTemplate;

@RestController
@RequestMapping("/dynamicFormFieldTemplate")
@Tag(name = "cat_dynamic_form_field_template")
public class DynamicFormFieldTemplateController {

    @Resource
    private DynamicFormFieldTemplateService dynamicFormFieldTemplateService;

    @Operation(summary = "添加")
    @RequestMapping(value = "/add",method = RequestMethod.POST)
    public HttpResult<?> add(@RequestBody DynamicFormFieldTemplate dynamicFormFieldTemplate) {
        return HttpResult.back(dynamicFormFieldTemplateService.add(dynamicFormFieldTemplate) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "删除")
    @RequestMapping(value = "/remove",method = RequestMethod.POST)
    public HttpResult<?> remove(@RequestBody DynamicFormFieldTemplate dynamicFormFieldTemplate) {
        return HttpResult.back(dynamicFormFieldTemplateService.delete(dynamicFormFieldTemplate) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "修改")
    @RequestMapping(value = "/update",method = RequestMethod.POST)
    public HttpResult<?> update(@RequestBody DynamicFormFieldTemplate dynamicFormFieldTemplate) {
        return HttpResult.back(dynamicFormFieldTemplateService.update(dynamicFormFieldTemplate) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "详情")
    @RequestMapping(value = "/info",method = RequestMethod.POST)
    public HttpResult<DynamicFormFieldTemplate> info(@RequestBody DynamicFormFieldTemplate dynamicFormFieldTemplate) {
        return HttpResult.back(dynamicFormFieldTemplateService.info(dynamicFormFieldTemplate));
    }

    @Operation(summary = "分页")
    @RequestMapping(value = "/queryPage",method = RequestMethod.POST)
    public HttpResult<Page<DynamicFormFieldTemplate>> queryPage(@RequestBody PageParam pageParam) {
        return HttpResult.back(dynamicFormFieldTemplateService.queryPage(pageParam));
    }



}
