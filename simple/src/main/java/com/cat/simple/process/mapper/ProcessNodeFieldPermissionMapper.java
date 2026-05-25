package com.cat.simple.process.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.process.ProcessNodeFieldPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProcessNodeFieldPermissionMapper extends BaseMapper<ProcessNodeFieldPermission> {

    void deletePhysicsByDefAndVersion(@Param("processDefinitionId") Integer processDefinitionId,
                                      @Param("version") String version);

    void copyVersion(@Param("processDefinitionId") Integer processDefinitionId,
                     @Param("sourceVersion") String sourceVersion,
                     @Param("targetVersion") String targetVersion);
}
