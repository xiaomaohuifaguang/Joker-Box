package com.cat.simple.process.controller;

import com.cat.common.entity.*;
import com.cat.common.entity.process.ProcessDefinition;
import com.cat.common.entity.process.ProcessDefinitionBytearray;
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
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public HttpResult<?> add(@RequestBody ProcessDefinition processDefinition) throws ParserConfigurationException, IOException, SAXException {
        return HttpResult.back(processDefinitionService.add(processDefinition) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "保存")
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public HttpResult<?> save(@RequestBody ProcessDefinition processDefinition) throws ParserConfigurationException, IOException, SAXException {
        return HttpResult.back(processDefinitionService.save(processDefinition) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "发布部署")
    @RequestMapping(value = "/deploy", method = RequestMethod.POST)
    @Parameters({
            @Parameter(name = "id", description = "流程定义id", required = true)
    })
    public HttpResult<?> deploy(@RequestParam(required = true) Integer id) {
        return HttpResult.back(processDefinitionService.deploy(id));
    }

    @Operation(summary = "删除")
    @RequestMapping(value = "/remove", method = RequestMethod.POST)
    public HttpResult<?> remove(@RequestBody ProcessDefinition processDefinition) {
        return HttpResult.back(processDefinitionService.delete(processDefinition) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "停用")
    @RequestMapping(value = "/stop", method = RequestMethod.POST)
    public HttpResult<?> stop(@RequestBody ProcessDefinition processDefinition) {
        return HttpResult.back(processDefinitionService.stop(processDefinition) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "详情")
    @Parameters({
            @Parameter(name = "version", description = "版本号（不传默认DRAFT或当前发布版本）")
    })
    @RequestMapping(value = "/info", method = RequestMethod.POST)
    public HttpResult<ProcessDefinition> info(@RequestBody ProcessDefinition processDefinition,
                                              @RequestParam(required = false) String version) {
        return HttpResult.back(processDefinitionService.info(processDefinition, version));
    }

    @Operation(summary = "分页")
    @RequestMapping(value = "/queryPage", method = RequestMethod.POST)
    public HttpResult<Page<ProcessDefinition>> queryPage(@RequestBody PageParam pageParam) {
        return HttpResult.back(processDefinitionService.queryPage(pageParam));
    }

    @Operation(summary = "已部署流程列表")
    @RequestMapping(value = "/deployList", method = RequestMethod.POST)
    public HttpResult<List<ProcessDefinition>> deployList() {
        return HttpResult.back(processDefinitionService.deployList());
    }

    @Operation(summary = "版本列表")
    @Parameters({
            @Parameter(name = "processDefinitionId", description = "流程定义id", required = true)
    })
    @RequestMapping(value = "/versionList", method = RequestMethod.POST)
    public HttpResult<List<ProcessDefinitionBytearray>> versionList(@RequestParam Integer processDefinitionId) {
        return HttpResult.back(processDefinitionService.versionList(processDefinitionId));
    }

    @Operation(summary = "回滚到指定版本")
    @Parameters({
            @Parameter(name = "processDefinitionId", description = "流程定义id", required = true),
            @Parameter(name = "targetVersion", description = "目标版本号", required = true)
    })
    @RequestMapping(value = "/rollback", method = RequestMethod.POST)
    public HttpResult<?> rollback(@RequestParam Integer processDefinitionId,
                                  @RequestParam String targetVersion) {
        return HttpResult.back(processDefinitionService.rollback(processDefinitionId, targetVersion) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }
}
