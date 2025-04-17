package com.cat.simple.controller;

import com.cat.common.entity.*;
import com.cat.common.entity.workOrder.WorkOrder;
import com.cat.common.entity.workOrder.WorkOrderPageParam;
import com.cat.simple.service.WorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/workOrder")
@Tag(name = "工单")
public class WorkOrderController {

    @Resource
    private WorkOrderService workOrderService;

    @Operation(summary = "详情")
    @RequestMapping(value = "/info",method = RequestMethod.POST)
    public HttpResult<WorkOrder> info(@RequestBody WorkOrder workOrder) {
        return HttpResult.back(workOrderService.info(workOrder));
    }

    @Operation(summary = "分页")
    @RequestMapping(value = "/queryPage",method = RequestMethod.POST)
    public HttpResult<Page<WorkOrder>> queryPage(@RequestBody WorkOrderPageParam pageParam) {
        return HttpResult.back(workOrderService.queryPage(pageParam));
    }

    @Operation(summary = "保存工单")
    @RequestMapping(value = "/draft",method = RequestMethod.POST)
    public HttpResult<?> draft(@RequestBody WorkOrder workOrder) {
        return HttpResult.back(workOrderService.draft(workOrder) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "发起审批")
    @RequestMapping(value = "/start",method = RequestMethod.POST)
    public HttpResult<?> start(@RequestBody WorkOrder workOrder) {
        return HttpResult.back(workOrderService.start(workOrder) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }


    @Operation(summary = "通过")
    @RequestMapping(value = "/pass",method = RequestMethod.POST)
    public HttpResult<?> pass(@RequestBody WorkOrder workOrder) {
        return HttpResult.back(workOrderService.pass(workOrder) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "转办")
    @RequestMapping(value = "/transfer",method = RequestMethod.POST)
    public HttpResult<?> transfer(@RequestBody WorkOrder workOrder,  @RequestParam Integer userId) {
        return HttpResult.back(workOrderService.transfer(workOrder, userId) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "拒绝")
    @RequestMapping(value = "/reject",method = RequestMethod.POST)
    public HttpResult<?> reject(@RequestBody WorkOrder workOrder) {
        return HttpResult.back(workOrderService.reject(workOrder) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }





}
