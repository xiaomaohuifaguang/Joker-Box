package com.cat.simple.controller;


import com.cat.common.entity.HttpResult;
import com.cat.common.entity.statisticalCenter.ChartData;
import com.cat.simple.service.StatisticalCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/statisticalCenter")
@Tag(name = "统计中心")
public class StatisticalCenterController {



    @Resource
    private StatisticalCenterService statisticalCenterService;


    @Operation(summary = "用户统计")
    @RequestMapping(value = "/peopleCount", method = RequestMethod.POST)
    public HttpResult<Map<String, Object>> peopleCount() {
        return HttpResult.back(statisticalCenterService.peopleCount());
    }

    @Operation(summary = "图表/用户近?天创建")
    @RequestMapping(value = "/peopleCreateByDay", method = RequestMethod.POST)
    public HttpResult<ChartData> peopleCreateByDay() {
        return HttpResult.back(statisticalCenterService.peopleCreateByDay());
    }



    @Operation(summary = "api请求统计前10")
    @RequestMapping(value = "/apiReqTotal", method = RequestMethod.POST)
    public HttpResult<ChartData> apiReqTotal() {
        return HttpResult.back(statisticalCenterService.apiReqTotal());
    }



}
