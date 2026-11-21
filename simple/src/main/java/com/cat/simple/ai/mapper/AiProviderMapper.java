package com.cat.simple.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.ai.provider.AiProvider;
import org.apache.ibatis.annotations.Mapper;

import com.cat.common.entity.Page;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiProviderMapper extends BaseMapper<AiProvider> {
    Page<AiProvider> selectPage(@Param("page") Page<AiProvider> page, @Param("param") PageParam pageParam);
}
