package com.cat.simple.gandashi.mapper;

import com.cat.common.entity.ganDaShi.GanDaShiComment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.ganDaShi.GanDaShiCommentPageParam;
import org.apache.ibatis.annotations.Mapper;

import com.cat.common.entity.Page;
import org.apache.ibatis.annotations.Param;


/**
 * <p>
 * 干大事帖子评论 Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-04-18
 */
@Mapper
public interface GanDaShiCommentMapper extends BaseMapper<GanDaShiComment> {
   Page<GanDaShiComment> selectPage(@Param("page") Page<GanDaShiComment> page, @Param("param") GanDaShiCommentPageParam param);
}
