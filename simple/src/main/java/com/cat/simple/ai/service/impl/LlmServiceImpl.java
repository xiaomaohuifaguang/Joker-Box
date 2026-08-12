package com.cat.simple.ai.service.impl;

import com.cat.common.entity.ai.model.AiModel;
import com.cat.common.entity.ai.model.ModelType;
import com.cat.simple.ai.service.AiModelService;
import com.cat.simple.ai.service.LlmService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class LlmServiceImpl implements LlmService {


    @Resource
    private AiModelService aiModelService;


    @Override
    public List<Float> vector(String text) {

        AiModel aiModel = aiModelService.defaultByTypeDecryptApiKey(ModelType.EMBEDDING.getCode());
        OpenAiApi openAiApi = buildOpenAiApi(aiModel);
        String model_name = aiModel.getModel();
        Integer dimension = aiModel.getDimension();
        OpenAiEmbeddingModel openAiEmbeddingModel = new OpenAiEmbeddingModel(openAiApi);
        EmbeddingRequest embeddingRequest = new EmbeddingRequest(List.of(text), EmbeddingOptions.builder().model(model_name).dimensions(dimension).build());
        EmbeddingResponse response = openAiEmbeddingModel.call(embeddingRequest);
        float[] output = response.getResult().getOutput();
        List<Float> list = new ArrayList<>(output.length);
        for (float f : output) {
            list.add(f);
        }
        return list;
    }



    @Override
    public OpenAiApi buildOpenAiApi(AiModel aiModel){
        String base_url = aiModel.getBaseUrl();
        String api_key = aiModel.getApiKey();
        String embeddingsPath = aiModel.getEmbeddingsPath();
        String completionsPath = aiModel.getCompletionsPath();
        OpenAiApi.Builder builder = OpenAiApi.builder().baseUrl(base_url).apiKey(api_key);
        if(StringUtils.hasText(completionsPath)){
            builder.completionsPath(completionsPath);
        }
        if(StringUtils.hasText(embeddingsPath)){
            builder.embeddingsPath(embeddingsPath);
        }
        return builder.build();
    }

}
