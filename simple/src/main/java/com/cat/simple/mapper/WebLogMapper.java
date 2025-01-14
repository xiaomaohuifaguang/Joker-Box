package com.cat.simple.mapper;

import com.cat.common.entity.WebLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import org.apache.ibatis.annotations.Param;


/**
 * <p>
 * web日志表 Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-01-06
 */
@Mapper
public interface WebLogMapper extends BaseMapper<WebLog> {
   Page<WebLog> selectPage(@Param("page") Page<WebLog> page);

   int bak(@Param("endDate") String endDate);
}