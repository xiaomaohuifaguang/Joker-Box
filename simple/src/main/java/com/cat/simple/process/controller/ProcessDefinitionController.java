package com.cat.simple.process.controller;

import com.cat.common.entity.*;
import com.cat.common.entity.process.ProcessDefinition;
import com.cat.simple.process.service.ProcessDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/processDefinition")
@Tag(name = "流程引擎")
public class ProcessDefinitionController {

@Resource
private ProcessDefinitionService processDefinitionService;

    @Operation(summary = "添加")
    @RequestMapping(value = "/add",method = RequestMethod.POST)
    public HttpResult<?> add(@RequestBody ProcessDefinition processDefinition) throws ParserConfigurationException, IOException, SAXException {
        return HttpResult.back(processDefinitionService.add(processDefinition) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }


    @Operation(summary = "保存")
    @RequestMapping(value = "/save",method = RequestMethod.POST)
    public HttpResult<?> save(@RequestBody ProcessDefinition processDefinition) throws ParserConfigurationException, IOException, SAXException {
        return HttpResult.back(processDefinitionService.save(processDefinition) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "发布部署")
    @RequestMapping(value = "/deploy",method = RequestMethod.POST)
    @Parameters({
            @Parameter(name = "id", description = "流程定义id",required = true)
    })
    public HttpResult<?> save(@RequestParam(required = true) Integer id){
        return HttpResult.back(processDefinitionService.deploy(id));
    }

    @Operation(summary = "删除")
    @RequestMapping(value = "/remove",method = RequestMethod.POST)
    public HttpResult<?> remove(@RequestBody ProcessDefinition processDefinition) {
        return HttpResult.back(processDefinitionService.delete(processDefinition) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "停用")
    @RequestMapping(value = "/stop",method = RequestMethod.POST)
    public HttpResult<?> stop(@RequestBody ProcessDefinition processDefinition) {
        return HttpResult.back(processDefinitionService.stop(processDefinition) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "详情")
    @RequestMapping(value = "/info",method = RequestMethod.POST)
    public HttpResult<ProcessDefinition> info(@RequestBody ProcessDefinition processDefinition) {
        return HttpResult.back(processDefinitionService.info(processDefinition));
    }

    @Operation(summary = "分页")
    @RequestMapping(value = "/queryPage",method = RequestMethod.POST)
    public HttpResult<Page<ProcessDefinition>> queryPage(@RequestBody PageParam pageParam) {
        return HttpResult.back(processDefinitionService.queryPage(pageParam));
    }


    @Operation(summary = "已部署流程列表")
    @RequestMapping(value = "/deployList",method = RequestMethod.POST)
    public HttpResult<List<ProcessDefinition>> deployList() {
        return HttpResult.back(processDefinitionService.deployList());
    }






//    @Operation(summary = "test")
//    @RequestMapping(value = "/test",method = RequestMethod.POST)
//    public HttpResult<?> test(@RequestParam(value = "processId",required = true) String processId) {
//        return HttpResult.back(processDefinitionService.test(processId));
//    }
//
//    @Operation(summary = "test/go")
//    @RequestMapping(value = "test/go",method = RequestMethod.POST)
//    public HttpResult<?> testGo(@RequestParam(value = "processInstanceId",required = true) String processInstanceId) {
//        return HttpResult.back(processDefinitionService.testGo(processInstanceId));
//    }
//
//
//    @Operation(summary = "test/back")
//    @RequestMapping(value = "test/back",method = RequestMethod.POST)
//    public HttpResult<?> testBack(@RequestParam(value = "processInstanceId",required = true) String processInstanceId) {
//        return HttpResult.back(processDefinitionService.testBack(processInstanceId));
//    }




}
