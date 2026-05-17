package com.cat.simple.ai.mapper;

import com.cat.common.entity.system.SystemPrompt;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import com.cat.common.entity.Page;
import org.apache.ibatis.annotations.Param;


/**
 * <p>
 * 系统提示 Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-04-16
 */
@Mapper
public interface SystemPromptMapper extends BaseMapper<SystemPrompt> {
   Page<SystemPrompt> selectPage(@Param("page") Page<SystemPrompt> page);
}
