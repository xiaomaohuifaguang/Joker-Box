package com.cat.simple.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.ai.chat.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {



}
