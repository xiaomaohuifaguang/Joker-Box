package com.cat.simple.mapper;

import com.cat.common.entity.process.ProcessHandleInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import com.cat.common.entity.Page;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * <p>
 * 流程处理记录 Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-04-13
 */
@Mapper
public interface ProcessHandleInfoMapper extends BaseMapper<ProcessHandleInfo> {
   Page<ProcessHandleInfo> selectPage(@Param("page") Page<ProcessHandleInfo> page);


   List<ProcessHandleInfo> selectDetailListByProcessInstanceId(@Param("processInstanceId") Integer processInstanceId);

   Integer selectMaxRound(@Param("processInstanceId") Integer processInstanceId,
                          @Param("taskDefinitionKey") String taskDefinitionKey);
}
