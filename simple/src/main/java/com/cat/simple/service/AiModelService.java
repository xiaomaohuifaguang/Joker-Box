package com.cat.simple.service;


import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.ai.model.AiModel;

public interface AiModelService {

    boolean add(AiModel aiModel);

    boolean delete(AiModel aiModel);

    boolean update(AiModel aiModel);

    AiModel info(AiModel aiModel);

    Page<AiModel> queryPage(PageParam pageParam);
}