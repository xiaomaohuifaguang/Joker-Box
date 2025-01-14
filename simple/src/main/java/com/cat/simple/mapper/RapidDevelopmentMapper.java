package com.cat.simple.mapper;

import com.cat.common.entity.rapidDevelopment.FieldInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RapidDevelopmentMapper {

    List<FieldInfo> queryAllFields(@Param("tableName") String tableName);


    List<String> tableNameList();

}
