package com.cat.simple.ai.service;

import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.ai.systemPrompt.AiSystemPrompt;

public interface AiSystemPromptService {

    boolean add(AiSystemPrompt aiSystemPrompt);

    boolean delete(Integer id);

    boolean update(AiSystemPrompt aiSystemPrompt);

    AiSystemPrompt info(Integer id);

    Page<AiSystemPrompt> queryPage(PageParam param);


}
