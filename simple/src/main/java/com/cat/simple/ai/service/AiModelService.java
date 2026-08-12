package com.cat.simple.ai.service;


import com.cat.common.entity.Page;
import com.cat.common.entity.ai.model.AiModel;
import com.cat.common.entity.ai.model.AiModelPageParam;
import com.cat.common.entity.ai.model.ModelType;

import java.util.List;
import java.util.Map;

public interface AiModelService {

    boolean add(AiModel aiModel);

    boolean delete(AiModel aiModel);

    boolean update(AiModel aiModel);

    AiModel info(AiModel aiModel);

    Page<AiModel> queryPage(AiModelPageParam pageParam);

    AiModel getOneWithRealApiKeyById(String id);


    Map<String, AiModel> defaultModel();

    AiModel defaultByType(String type);

    AiModel defaultByTypeDecryptApiKey(String type);

    boolean setDefaultModel(String type, String modelId);

    boolean clearDefaultModel(String type);

    List<AiModel> list(ModelType modelType);


}