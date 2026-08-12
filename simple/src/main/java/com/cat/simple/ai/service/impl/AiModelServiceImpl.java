package com.cat.simple.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.Page;
import com.cat.common.entity.ai.model.AiModel;
import com.cat.common.entity.ai.model.AiModelPageParam;
import com.cat.common.entity.ai.model.ModelType;
import com.cat.common.utils.crypto.AESUtils;
import com.cat.simple.config.cache.CacheKeyEnum;
import com.cat.simple.config.cache.CacheService;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.ai.mapper.AiModelMapper;
import com.cat.simple.ai.service.AiModelService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AiModelServiceImpl implements AiModelService {


    @Resource
    private AiModelMapper aiModelMapper;

    @Value("${custom.aes.key}")
    private String aesKey;

    @Resource
    private CacheService cacheService;


    @Override
    public boolean add(AiModel aiModel){
        ModelType modelType = ModelType.of(aiModel.getType());
        if(Objects.isNull(modelType)){
            throw new IllegalStateException("模型类型错误");
        }
        String apiKey = aiModel.getApiKey();
        if(StringUtils.hasText(apiKey)){
            aiModel.setApiKey(AESUtils.encrypt(apiKey, aesKey));
        }
        aiModel.setUserId(Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId());
        aiModel.setCreateTime(LocalDateTime.now());
        return aiModelMapper.insert(aiModel) == 1;
    }

    @Override
    public boolean delete(AiModel aiModel){
        int defaultCount = aiModelMapper.selectDefaultCountByModelId(aiModel.getId());
        if(defaultCount > 0){
            throw new IllegalStateException("绑定默认模型请取消绑定后删除");
        }
        return aiModelMapper.deleteById(aiModel) == 1;
    }

    @Override
    public boolean update(AiModel aiModel){
        ModelType modelType = ModelType.of(aiModel.getType());
        if(Objects.isNull(modelType)){
            throw new IllegalStateException("模型类型错误");
        }

        AiModel original = aiModelMapper.selectById(aiModel.getId());
        String apiKey = aiModel.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            aiModel.setApiKey(original.getApiKey());
        } else {
            aiModel.setApiKey(AESUtils.encrypt(apiKey, aesKey));
        }
        aiModel.setUserId(original.getUserId());
        aiModel.setCreateTime(original.getCreateTime());
        int i = aiModelMapper.updateById(aiModel);
        clearDefaultModelCacheAll();
        return i == 1;
    }

    @Override
    public AiModel info(AiModel aiModel){
        AiModel result = aiModelMapper.selectById(aiModel.getId());
        if (result != null) {
            String decrypted = AESUtils.decrypt(result.getApiKey(), aesKey);
            result.setApiKey(maskApiKey(decrypted));
        }
        return result;
    }

    @Override
    public Page<AiModel> queryPage(AiModelPageParam pageParam){
        Page<AiModel> page = new Page<>(pageParam);
        page = aiModelMapper.selectPage(page, pageParam);
        List<AiModel> records = page.getRecords();
        if (records != null) {
            records.forEach(record -> {
                String decrypted = AESUtils.decrypt(record.getApiKey(), aesKey);
                record.setApiKey(maskApiKey(decrypted));
            });
        }
        return page;
    }

    @Override
    public AiModel getOneWithRealApiKeyById(String id) {
        AiModel aiModel = aiModelMapper.selectById(id);
        if (aiModel == null) {
            return null;
        }
        String decrypted = AESUtils.decrypt(aiModel.getApiKey(), aesKey);
        aiModel.setApiKey(decrypted);
        return aiModel;
    }

    @Override
    public Map<String, AiModel> defaultModel() {
        Map<String, AiModel> map = new HashMap<>();
        AiModel defaultChatModel = defaultByType(ModelType.CHAT.getCode());
        if(Objects.nonNull(defaultChatModel)){
            map.put(ModelType.CHAT.getCode(), defaultChatModel);
        }
        AiModel defaultEmbeddingModel = defaultByType(ModelType.EMBEDDING.getCode());
        if(Objects.nonNull(defaultEmbeddingModel)){
            map.put(ModelType.EMBEDDING.getCode(), defaultEmbeddingModel);
        }
        return map;
    }

    @Override
    public AiModel defaultByType(String type) {
        ModelType modelType = ModelType.of(type);
        if(Objects.isNull(modelType)){
            throw new IllegalStateException("模型类型错误");
        }
        AiModel aiModel;
        aiModel = cacheService.get( CacheKeyEnum.AI_MODEL_DEFAULT, type, AiModel.class);
        if(Objects.isNull(aiModel)){
            aiModel = aiModelMapper.selectDefaultByType(modelType.getCode());
            if(Objects.nonNull(aiModel)){
                cacheService.set(CacheKeyEnum.AI_MODEL_DEFAULT,  type, aiModel);
            }
        }
        return aiModel;
    }

    @Override
    public AiModel defaultByTypeDecryptApiKey(String type) {
        AiModel aiModel = defaultByType(type);
        if(Objects.nonNull(aiModel) && StringUtils.hasText(aiModel.getApiKey())){
            String decrypted = AESUtils.decrypt(aiModel.getApiKey(), aesKey);
            aiModel.setApiKey(decrypted);
        }
        return aiModel;
    }

    @Override
    public boolean setDefaultModel(String type, String modelId) {
        ModelType modelType = ModelType.of(type);
        if(Objects.isNull(modelType)){
            throw new IllegalStateException("模型类型错误");
        }

        AiModel aiModel = aiModelMapper.selectOne(new LambdaQueryWrapper<AiModel>().eq(AiModel::getId, modelId).eq(AiModel::getType, type));
        if(Objects.isNull(aiModel)){
            throw new IllegalStateException("模型不存在");
        }

        int i = aiModelMapper.insertOrUpdateDefault(type, modelId);
        clearDefaultModelCacheAll();
        return i > 0;
    }

    @Override
    public boolean clearDefaultModel(String type) {
        clearDefaultModelCacheAll();
        return aiModelMapper.deleteDefaultByType(type) == 1;
    }

    @Override
    public List<AiModel> list(ModelType modelType) {
        if(Objects.isNull(modelType)){
            return Collections.emptyList();
        }

        return aiModelMapper.selectList(new LambdaQueryWrapper<AiModel>().eq(AiModel::getType, modelType.getCode()));
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 4) {
            return apiKey;
        }
        return apiKey.substring(0, 2) + "*".repeat(apiKey.length() - 4) + apiKey.substring(apiKey.length() - 2);
    }


    private void clearDefaultModelCacheAll(){
        cacheService.deleteKey(CacheKeyEnum.AI_MODEL_DEFAULT, ModelType.CHAT.getCode());
        cacheService.deleteKey(CacheKeyEnum.AI_MODEL_DEFAULT, ModelType.EMBEDDING.getCode());
    }

}