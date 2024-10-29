package com.cat.simple.mapper;

import com.cat.common.entity.Page;
import com.cat.common.entity.website.Website;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.website.WebsitePageParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 网站收藏 Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2024-10-17
 */
@Mapper
public interface WebsiteMapper extends BaseMapper<Website> {

    Page<Website> selectPage(@Param("page") Page<Website> page, @Param("param") WebsitePageParam param);

}
