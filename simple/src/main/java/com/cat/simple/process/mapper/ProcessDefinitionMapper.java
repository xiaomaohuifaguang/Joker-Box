package com.cat.simple.process.mapper;

import com.cat.common.entity.process.ProcessDefinition;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import com.cat.common.entity.Page;
import org.apache.ibatis.annotations.Param;


/**
 * <p>
 * 流程定义信息表 Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-02-27
 */
@Mapper
public interface ProcessDefinitionMapper extends BaseMapper<ProcessDefinition> {
   Page<ProcessDefinition> selectPage(@Param("page") Page<ProcessDefinition> page);
}
