package com.cat.simple.service;


import com.cat.common.entity.statisticalCenter.ChartData;

import java.util.Map;

public interface StatisticalCenterService {

    Map<String, Object> peopleCount();


    ChartData peopleCreateByDay();

    ChartData apiReqTotal();


}
