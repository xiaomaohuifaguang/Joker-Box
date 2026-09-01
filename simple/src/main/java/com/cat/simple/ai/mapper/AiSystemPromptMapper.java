package com.cat.simple.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.ai.systemPrompt.AiSystemPrompt;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiSystemPromptMapper extends BaseMapper<AiSystemPrompt> {


    Page<AiSystemPrompt> selectPage(Page<AiSystemPrompt> page, PageParam param);


}
