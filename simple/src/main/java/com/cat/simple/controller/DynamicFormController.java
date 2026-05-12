package com.cat.simple.controller;

import com.cat.common.entity.*;
import com.cat.common.entity.dynamicForm.DynamicForm;
import com.cat.common.entity.dynamicForm.FormData;
import com.cat.simple.service.DynamicFormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dynamicForm")
@Tag(name = "动态表单")
public class DynamicFormController {

@Resource
private DynamicFormService dynamicFormService;

    @Operation(summary = "添加")
    @RequestMapping(value = "/add",method = RequestMethod.POST)
    public HttpResult<?> add(@RequestBody DynamicForm dynamicForm) {
        return HttpResult.back(dynamicFormService.add(dynamicForm) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "删除")
    @RequestMapping(value = "/remove",method = RequestMethod.POST)
    public HttpResult<?> remove(@RequestBody DynamicForm dynamicForm) {
        return HttpResult.back(dynamicFormService.delete(dynamicForm) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "修改")
    @RequestMapping(value = "/update",method = RequestMethod.POST)
    public HttpResult<?> update(@RequestBody DynamicForm dynamicForm) {
        return HttpResult.back(dynamicFormService.update(dynamicForm) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "详情")
    @RequestMapping(value = "/info",method = RequestMethod.POST)
    public HttpResult<DynamicForm> info(@RequestBody DynamicForm dynamicForm) {
        return HttpResult.back(dynamicFormService.info(dynamicForm));
    }

    @Operation(summary = "分页")
    @RequestMapping(value = "/queryPage",method = RequestMethod.POST)
    public HttpResult<Page<DynamicForm>> queryPage(@RequestBody PageParam pageParam) {
        return HttpResult.back(dynamicFormService.queryPage(pageParam));
    }

    @Operation(summary = "发布")
    @Parameters(
            @Parameter(name = "formId",description = "表单id",required = true)
    )
    @RequestMapping(value = "/deploy",method = RequestMethod.POST)
    public HttpResult<DynamicForm> deploy(@RequestParam(value = "formId",required = true) String formId) {
        return HttpResult.back(dynamicFormService.deploy(formId)  ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "停用")
    @Parameters(
            @Parameter(name = "formId",description = "表单id",required = true)
    )
    @RequestMapping(value = "/stop",method = RequestMethod.POST)
    public HttpResult<DynamicForm> stop(@RequestParam(value = "formId",required = true) String formId) {
        return HttpResult.back(dynamicFormService.stop(formId)  ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }


    @Operation(summary = "提交")
    @RequestMapping(value = "/submit",method = RequestMethod.POST)
    public HttpResult<?> submit(@RequestBody FormData formData) {
        return HttpResult.back(dynamicFormService.submit(formData)  ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }


}
