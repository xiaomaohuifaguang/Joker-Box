package com.cat.simple.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.auth.User;
import com.cat.common.entity.statisticalCenter.ChartData;
import com.cat.common.utils.datetime.DateTimeUtils;
import com.cat.simple.mapper.StatisticalCenterMapper;
import com.cat.simple.mapper.UserMapper;
import com.cat.simple.service.StatisticalCenterService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class StatisticalCenterServiceImpl implements StatisticalCenterService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private StatisticalCenterMapper statisticalCenterMapper;

    @Override
    public Map<String, Object> peopleCount() {

        HashMap<String, Object> result = new HashMap<>();

        Long total = userMapper.selectCount(new LambdaQueryWrapper<>());
        result.put("total", total);
        Long todayRegister = userMapper.selectCount(new LambdaQueryWrapper<User>().gt(User::getCreateTime, DateTimeUtils.getLocalDateByDay(0)));
        result.put("todayRegister", todayRegister);

        return result;
    }

    @Override
    public ChartData peopleCreateByDay() {

        ChartData chartData = new ChartData();

        int dayIn = -7;

        List<String> xData = new ArrayList<>();
        List<Object> yData = new ArrayList<>();

        for(int i = dayIn + 1 ; i <= 0; i++){
            xData.add(DateTimeUtils.getFormatStrByLocalDate(DateTimeUtils.getLocalDateByDay(i),DateTimeUtils.DATE_FORMAT_Y_M_D));
        }

        List<HashMap<String, Object>> maps = statisticalCenterMapper.registerCountByDate(DateTimeUtils.getLocalDateByDay(dayIn + 1));

        xData.forEach(x->{
            AtomicReference<Object> data = new AtomicReference<>(null);
            maps.forEach(m->{
                String day = String.valueOf(m.get("day"));
                if( x.equals(day)){
                   data.set(m.get("count"));
               }
            });
            if(data.get() == null){
                yData.add(0);
            }else {
                yData.add(data.get());
            }
        });






        return chartData.setXdata(xData).setYdata(yData);
    }

    @Override
    public ChartData apiReqTotal() {
        ChartData chartData = new ChartData();
        List<String> xData = new ArrayList<>();
        List<Object> yData = new ArrayList<>();
        List<HashMap<String, Object>> maps = statisticalCenterMapper.apiReqTotal();
        maps.forEach(m->{
            String name = String.valueOf(m.get("name"));
            xData.add(name);
            yData.add(m.get("count"));
        });
        return chartData.setXdata(xData).setYdata(yData);
    }


}
