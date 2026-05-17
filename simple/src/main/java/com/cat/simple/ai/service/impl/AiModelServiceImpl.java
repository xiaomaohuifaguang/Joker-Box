package com.cat.simple.ai.service.impl;

import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.ai.model.AiModel;
import com.cat.common.utils.crypto.AESUtils;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.ai.mapper.AiModelMapper;
import com.cat.simple.ai.service.AiModelService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class AiModelServiceImpl implements AiModelService {


    @Resource
    private AiModelMapper aiModelMapper;

    @Value("${custom.aes.key}")
    private String aesKey;

    @Override
    public boolean add(AiModel aiModel){
        String apiKey = aiModel.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException("API密钥不能为空");
        }
        aiModel.setApiKey(AESUtils.encrypt(apiKey, aesKey));
        aiModel.setUserId(Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId());
        aiModel.setCreateTime(LocalDateTime.now());
        return aiModelMapper.insert(aiModel) == 1;
    }

    @Override
    public boolean delete(AiModel aiModel){
            return aiModelMapper.deleteById(aiModel) == 1;
    }

    @Override
    public boolean update(AiModel aiModel){
        AiModel original = aiModelMapper.selectById(aiModel.getId());
        String apiKey = aiModel.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            aiModel.setApiKey(original.getApiKey());
        } else {
            aiModel.setApiKey(AESUtils.encrypt(apiKey, aesKey));
        }
        aiModel.setUserId(original.getUserId());
        aiModel.setCreateTime(original.getCreateTime());
        return aiModelMapper.updateById(aiModel) == 1;
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
    public Page<AiModel> queryPage(PageParam pageParam){
        Page<AiModel> page = new Page<>(pageParam);
        page = aiModelMapper.selectPage(page);
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

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 4) {
            return apiKey;
        }
        return apiKey.substring(0, 2) + "*".repeat(apiKey.length() - 4) + apiKey.substring(apiKey.length() - 2);
    }
}