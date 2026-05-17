package com.cat.simple.ai.service;


import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.system.SystemPrompt;

import java.util.List;

public interface SystemPromptService {

    boolean add(SystemPrompt systemPrompt);

    boolean delete(SystemPrompt systemPrompt);


    SystemPrompt info(SystemPrompt systemPrompt);

    Page<SystemPrompt> queryPage(PageParam pageParam);

    List<SystemPrompt> queryAll();
}