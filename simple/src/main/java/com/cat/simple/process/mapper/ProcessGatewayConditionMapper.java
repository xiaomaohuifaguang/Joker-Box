package com.cat.simple.process.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.process.ProcessGatewayCondition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProcessGatewayConditionMapper extends BaseMapper<ProcessGatewayCondition> {

    void copyVersion(@Param("processDefinitionId") Integer processDefinitionId,
                     @Param("sourceVersion") String sourceVersion,
                     @Param("targetVersion") String targetVersion);

    void deletePhysicsByDefAndVersion(@Param("processDefinitionId") Integer processDefinitionId,
                                      @Param("version") String version);
}
