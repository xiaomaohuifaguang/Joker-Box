package com.cat.simple.service.impl;

import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.ai.model.AiModel;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.mapper.AiModelMapper;
import com.cat.simple.service.AiModelService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class AiModelServiceImpl implements AiModelService {


    @Resource
    private AiModelMapper aiModelMapper;

    @Override
    public boolean add(AiModel aiModel){
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
        aiModel.setUserId(original.getUserId());
        aiModel.setCreateTime(original.getCreateTime());
        return aiModelMapper.updateById(aiModel) == 1;
    }

    @Override
    public AiModel info(AiModel aiModel){
        return  aiModelMapper.selectById(aiModel.getId());
    }

    @Override
    public Page<AiModel> queryPage(PageParam pageParam){
        Page<AiModel> page = new Page<>(pageParam);
        page = aiModelMapper.selectPage(page);
        return page;
    }
}