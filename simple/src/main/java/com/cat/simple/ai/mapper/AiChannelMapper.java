package com.cat.simple.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.ai.channel.AiChannel;
import org.apache.ibatis.annotations.Mapper;

import com.cat.common.entity.Page;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiChannelMapper extends BaseMapper<AiChannel> {
    Page<AiChannel> selectPage(@Param("page") Page<AiChannel> page, @Param("param")PageParam pageParam);

    AiChannel selectById(@Param("id") Integer id);


}
