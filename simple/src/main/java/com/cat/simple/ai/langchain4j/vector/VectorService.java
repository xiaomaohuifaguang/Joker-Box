package com.cat.simple.ai.langchain4j.vector;

import com.cat.common.entity.ai.model.AiModel;
import com.cat.common.entity.ai.model.ModelType;
import com.cat.simple.ai.langchain4j.ModelBuilder;
import com.cat.simple.ai.service.AiModelService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VectorService {

    @Resource
    private ModelBuilder modelBuilder;
    @Resource
    private AiModelService aiModelService;


    public List<Float> vector(String text){

        AiModel aiModel = aiModelService.defaultByTypeDecryptApiKey(ModelType.EMBEDDING.getCode());
        EmbeddingModel embeddingModel = modelBuilder.makeEmbeddingModel(aiModel);
        return embeddingModel.embed(text).content().vectorAsList();

    }


}
