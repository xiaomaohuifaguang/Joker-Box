package com.cat.simple.ai.service;


import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.ai.provider.AiProvider;

public interface AiProviderService {

    boolean add(AiProvider aiProvider);

    boolean delete(AiProvider aiProvider);

    boolean update(AiProvider aiProvider);

    AiProvider info(AiProvider aiProvider);

    Page<AiProvider> queryPage(PageParam pageParam);
}