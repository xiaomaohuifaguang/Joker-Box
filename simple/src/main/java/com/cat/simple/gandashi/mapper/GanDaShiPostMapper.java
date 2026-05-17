package com.cat.simple.gandashi.mapper;

import com.cat.common.entity.PageParam;
import com.cat.common.entity.ganDaShi.GanDaShiPost;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import com.cat.common.entity.Page;
import org.apache.ibatis.annotations.Param;


/**
 * <p>
 * 干大事主贴 Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-04-18
 */
@Mapper
public interface GanDaShiPostMapper extends BaseMapper<GanDaShiPost> {
   Page<GanDaShiPost> selectPage(@Param("page") Page<GanDaShiPost> page, @Param("param") PageParam pageParam);

   GanDaShiPost selectById(@Param("id") Integer id);

}
