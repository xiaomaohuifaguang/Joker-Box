package com.cat.simple.ai.service;

import com.cat.common.entity.ai.model.AiModel;
import org.springframework.ai.openai.api.OpenAiApi;

import java.util.List;

public interface LlmService {

    List<Float> vector(String text);

    OpenAiApi buildOpenAiApi(AiModel aiModel);

}
