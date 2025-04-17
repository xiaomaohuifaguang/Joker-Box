package com.cat.simple.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.workOrder.WorkOrder;
import com.cat.common.entity.workOrder.WorkOrderPageParam;
import org.apache.ibatis.annotations.Mapper;

import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import org.apache.ibatis.annotations.Param;


/**
 * <p>
 * 工单主要信息 Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-04-12
 */
@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {
   Page<WorkOrder> selectPage(@Param("page") Page<WorkOrder> page, @Param("param") WorkOrderPageParam param);


   WorkOrder selectDetailById(@Param("id") Integer id);


}
