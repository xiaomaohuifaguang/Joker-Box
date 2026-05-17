package com.cat.simple.info.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface BootMapper {



    List<Map<String, String>> list();



}
