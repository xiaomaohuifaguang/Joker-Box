package com.cat.simple.mapper;

import com.cat.common.entity.process.ProcessInstance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.process.ProcessInstancePageParam;
import org.apache.ibatis.annotations.Mapper;

import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import org.apache.ibatis.annotations.Param;


/**
 * <p>
 * 流程实例表 Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-03-06
 */
@Mapper
public interface ProcessInstanceMapper extends BaseMapper<ProcessInstance> {
   Page<ProcessInstance> selectPage(@Param("page") Page<ProcessInstance> page, @Param("param")ProcessInstancePageParam param);


   ProcessInstance selectInfoById(@Param("id") Integer id);

}
