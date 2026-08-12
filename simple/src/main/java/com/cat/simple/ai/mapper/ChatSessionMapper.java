package com.cat.simple.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.ai.chat.ChatSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

}
