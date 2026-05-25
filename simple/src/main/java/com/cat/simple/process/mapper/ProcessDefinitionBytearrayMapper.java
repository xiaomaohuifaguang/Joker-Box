package com.cat.simple.process.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.process.ProcessDefinitionBytearray;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProcessDefinitionBytearrayMapper extends BaseMapper<ProcessDefinitionBytearray> {

    void deletePhysicsByDefAndVersion(@Param("processDefinitionId") Integer processDefinitionId,
                                      @Param("version") String version);

    void copyVersion(@Param("processDefinitionId") Integer processDefinitionId,
                     @Param("sourceVersion") String sourceVersion,
                     @Param("targetVersion") String targetVersion);
}
