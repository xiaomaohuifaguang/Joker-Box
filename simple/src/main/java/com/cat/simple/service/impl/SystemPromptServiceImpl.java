package com.cat.simple.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.system.SystemPrompt;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.mapper.SystemPromptMapper;
import com.cat.simple.mapper.UserMapper;
import com.cat.simple.service.SystemPromptService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class SystemPromptServiceImpl implements SystemPromptService {


    @Resource
    private SystemPromptMapper systemPromptMapper;
    @Resource
    private UserMapper userMapper;

    @Override
    public boolean add(SystemPrompt systemPrompt){
        systemPrompt.setCreateBy(Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId());
        systemPrompt.setCreateTime(LocalDateTime.now());
        systemPrompt.setDeleted("0");
        return systemPromptMapper.insert(systemPrompt) == 1;
    }

    @Override
    public boolean delete(SystemPrompt systemPrompt){
            return systemPromptMapper.deleteById(systemPrompt) == 1;
    }


    @Override
    public SystemPrompt info(SystemPrompt systemPrompt){
        systemPrompt = systemPromptMapper.selectById(systemPrompt.getId());
        systemPrompt.setCreateByName(userMapper.selectById(systemPrompt.getCreateBy()).getNickname());
        return systemPrompt;
    }

    @Override
    public Page<SystemPrompt> queryPage(PageParam pageParam){
        Page<SystemPrompt> page = new Page<>(pageParam);
        page = systemPromptMapper.selectPage(page);
        return page;
    }

    @Override
    public List<SystemPrompt> queryAll() {
        return systemPromptMapper.selectList(new LambdaQueryWrapper<SystemPrompt>()
                .select(SystemPrompt::getPrompt)
                .gt(SystemPrompt::getDeadTime, LocalDateTime.now()));
    }
}