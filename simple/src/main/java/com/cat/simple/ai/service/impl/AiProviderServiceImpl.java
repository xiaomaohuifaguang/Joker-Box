package com.cat.simple.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.ai.channel.AiChannel;
import com.cat.common.entity.ai.provider.AiProvider;
import com.cat.simple.ai.mapper.AiChannelMapper;
import com.cat.simple.ai.mapper.AiProviderMapper;
import com.cat.simple.ai.service.AiChannelService;
import com.cat.simple.ai.service.AiProviderService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AiProviderServiceImpl implements AiProviderService {


    @Resource
    private AiProviderMapper aiProviderMapper;
    @Resource
    private AiChannelMapper aiChannelMapper;
    @Resource
    private AiChannelService aiChannelService;

    @Override
    public boolean add(AiProvider aiProvider){
        return aiProviderMapper.insert(aiProvider) == 1;
    }

    @Override
    @Transactional
    public boolean delete(AiProvider aiProvider){
        List<AiChannel> aiChannels = aiChannelMapper.selectList(new LambdaQueryWrapper<AiChannel>().eq(AiChannel::getProviderId, aiProvider.getId()));

        for (AiChannel aiChannel : aiChannels) {
            aiChannelService.delete(aiChannel);
        }

        return aiProviderMapper.deleteById(aiProvider) == 1;
    }

    @Override
    public boolean update(AiProvider aiProvider){
        return aiProviderMapper.updateById(aiProvider) == 1;
    }

    @Override
    public AiProvider info(AiProvider aiProvider){
        return  aiProviderMapper.selectById(aiProvider.getId());
    }

    @Override
    public Page<AiProvider> queryPage(PageParam pageParam){
        Page<AiProvider> page = new Page<>(pageParam);
        page = aiProviderMapper.selectPage(page, pageParam);
        return page;
    }
}