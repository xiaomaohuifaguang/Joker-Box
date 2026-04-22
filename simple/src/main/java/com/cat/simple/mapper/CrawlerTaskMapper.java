package com.cat.simple.mapper;

import com.cat.common.entity.crawler.CrawlerTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import com.cat.common.entity.Page;
import org.apache.ibatis.annotations.Param;


/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-05-08
 */
@Mapper
public interface CrawlerTaskMapper extends BaseMapper<CrawlerTask> {
   Page<CrawlerTask> selectPage(@Param("page") Page<CrawlerTask> page);
}
