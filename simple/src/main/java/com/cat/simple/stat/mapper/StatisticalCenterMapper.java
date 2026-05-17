package com.cat.simple.stat.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper
public interface StatisticalCenterMapper {

    List<HashMap<String, Object>> registerCountByDate(@Param("lastDate") LocalDate lastDate);

    List<HashMap<String, Object>> apiReqTotal();

}
