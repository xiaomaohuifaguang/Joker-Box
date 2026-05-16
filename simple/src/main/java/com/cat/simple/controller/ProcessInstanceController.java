package com.cat.simple.controller;

import com.cat.common.entity.HttpResult;
import com.cat.common.entity.HttpResultStatus;
import com.cat.common.entity.Page;
import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.common.entity.process.BackTargetNode;
import com.cat.common.entity.process.ProcessBackParam;
import com.cat.common.entity.process.ProcessInstancePageParam;
import com.cat.simple.service.ProcessBackService;
import com.cat.simple.service.ProcessInstanceService;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/processInstance")
@Tag(name = "流程实例")
public class ProcessInstanceController {

    @Resource
    private ProcessInstanceService processInstanceService;

    @Resource
    private ProcessBackService processBackService;


    @Operation(summary = "发起流程")
    @Parameters({
            @Parameter(name = "processDefinitionId", description = "自建流程定义id", required = true),
            @Parameter(name = "title", description = "流程标题")
    })
    @RequestMapping(value = "/start", method = RequestMethod.POST)
    public HttpResult<ProcessInstance> start(@RequestParam("processDefinitionId") Integer processDefinitionId,
                                              @RequestParam(value = "title", required = false) String title) {
        return HttpResult.back(processInstanceService.start(processDefinitionId, title));
    }


    @Operation(summary = "分页")
    @RequestMapping(value = "/queryPage", method = RequestMethod.POST)
    public HttpResult<Page<ProcessInstance>> queryPage(@RequestBody ProcessInstancePageParam pageParam) {
        return HttpResult.back(processInstanceService.queryPage(pageParam));
    }


    @Operation(summary = "详情")
    @Parameters({
            @Parameter(name = "id", description = "自建流程实例id", required = true)
    })
    @RequestMapping(value = "/info", method = RequestMethod.POST)
    public HttpResult<ProcessInstance> info(@RequestParam("id") Integer id) {
        return HttpResult.back(processInstanceService.info(id));
    }


    @Operation(summary = "认领任务")
    @RequestMapping(value = "/claim", method = RequestMethod.POST)
    public HttpResult<?> claim(@RequestBody ProcessHandleParam param) {
        processInstanceService.claim(param);
        return HttpResult.back(HttpResultStatus.SUCCESS);
    }


    @Operation(summary = "审批通过")
    @RequestMapping(value = "/pass", method = RequestMethod.POST)
    public HttpResult<?> pass(@RequestBody ProcessHandleParam param) {
        processInstanceService.pass(param);
        return HttpResult.back(HttpResultStatus.SUCCESS);
    }

    @Operation(summary = "拒绝")
    @RequestMapping(value = "/reject", method = RequestMethod.POST)
    public HttpResult<?> reject(@RequestBody ProcessHandleParam param) {
        processInstanceService.reject(param);
        return HttpResult.back(HttpResultStatus.SUCCESS);
    }


    @Operation(summary = "保存草稿")
    @Parameters({
            @Parameter(name = "id", description = "草稿流程实例id，传则更新，不传则新建"),
            @Parameter(name = "processDefinitionId", description = "自建流程定义id", required = true),
            @Parameter(name = "title", description = "流程标题")
    })
    @RequestMapping(value = "/saveDraft", method = RequestMethod.POST)
    public HttpResult<ProcessInstance> saveDraft(@RequestParam(value = "id", required = false) Integer id,
                                                  @RequestParam("processDefinitionId") Integer processDefinitionId,
                                                  @RequestParam(value = "title", required = false) String title) {
        return HttpResult.back(processInstanceService.saveDraft(id, processDefinitionId, title));
    }

    @Operation(summary = "驳回")
    @RequestMapping(value = "/back", method = RequestMethod.POST)
    public HttpResult<?> back(@RequestBody ProcessBackParam param) {
        processBackService.back(param);
        return HttpResult.back(HttpResultStatus.SUCCESS);
    }

    @Operation(summary = "查询当前任务可驳回的目标节点")
    @RequestMapping(value = "/availableBackTargets", method = RequestMethod.POST)
    public HttpResult<List<BackTargetNode>> availableBackTargets(@RequestParam String taskId) {
        return HttpResult.back(processBackService.getAvailableBackTargets(taskId));
    }

}