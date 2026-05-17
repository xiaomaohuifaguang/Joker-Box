package com.cat.simple.log.service.impl;

import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.WebLog;
import com.cat.simple.log.mapper.WebLogMapper;
import com.cat.simple.log.service.WebLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class WebLogServiceImpl implements WebLogService {


    @Resource
    private WebLogMapper webLogMapper;

    @Override
    public boolean add(WebLog webLog){
        return webLogMapper.insert(webLog) == 1;
    }

    @Override
    public boolean delete(WebLog webLog){
            return webLogMapper.deleteById(webLog) == 1;
    }

    @Override
    public boolean update(WebLog webLog){
        return webLogMapper.updateById(webLog) == 1;
    }

    @Override
    public WebLog info(WebLog webLog){
        return  webLogMapper.selectById(webLog.getId());
    }

    @Override
    public Page<WebLog> queryPage(PageParam pageParam){
        Page<WebLog> page = new Page<>(pageParam);
        page = webLogMapper.selectPage(page);
        return page;
    }
}