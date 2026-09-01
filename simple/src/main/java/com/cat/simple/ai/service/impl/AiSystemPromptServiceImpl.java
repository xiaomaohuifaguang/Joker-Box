package com.cat.simple.ai.service.impl;

import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.ai.systemPrompt.AiSystemPrompt;
import com.cat.simple.ai.mapper.AiSystemPromptMapper;
import com.cat.simple.ai.service.AiSystemPromptService;
import com.cat.simple.config.security.SecurityUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Slf4j
public class AiSystemPromptServiceImpl implements AiSystemPromptService {

    @Resource
    private AiSystemPromptMapper aiSystemPromptMapper;


    @Override
    public boolean add(AiSystemPrompt aiSystemPrompt) {
        aiSystemPrompt.setId(null);
        aiSystemPrompt.setCreateBy(Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId());
        aiSystemPrompt.setCreateTime(LocalDateTime.now());
        aiSystemPrompt.setUpdateTime(aiSystemPrompt.getCreateTime());
        return aiSystemPromptMapper.insert(aiSystemPrompt) == 1;
    }

    @Override
    public boolean delete(Integer id) {
        return aiSystemPromptMapper.deleteById(id) == 1;
    }

    @Override
    public boolean update(AiSystemPrompt aiSystemPrompt) {
        AiSystemPrompt ori = aiSystemPromptMapper.selectById(aiSystemPrompt.getId());
        ori.setPrompt(aiSystemPrompt.getPrompt());
        ori.setUpdateTime(LocalDateTime.now());
        return aiSystemPromptMapper.updateById(ori) == 1;
    }

    @Override
    public AiSystemPrompt info(Integer id) {
        return aiSystemPromptMapper.selectById(id);
    }

    @Override
    public Page<AiSystemPrompt> queryPage(PageParam param) {

        Page<AiSystemPrompt> page = new Page<>(param);

        page = aiSystemPromptMapper.selectPage(page, param);

        return page;
    }
}
