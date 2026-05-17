package com.cat.simple.crawler.service.impl;

import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.crawler.CrawlerTask;
import com.cat.simple.crawler.mapper.CrawlerTaskMapper;
import com.cat.simple.crawler.service.CrawlerTaskService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class CrawlerTaskServiceImpl implements CrawlerTaskService {


    @Resource
    private CrawlerTaskMapper crawlerTaskMapper;

    @Override
    public boolean add(CrawlerTask crawlerTask){
        return crawlerTaskMapper.insert(crawlerTask) == 1;
    }

    @Override
    public boolean delete(CrawlerTask crawlerTask){
            return crawlerTaskMapper.deleteById(crawlerTask) == 1;
    }

    @Override
    public boolean update(CrawlerTask crawlerTask){
        return crawlerTaskMapper.updateById(crawlerTask) == 1;
    }

    @Override
    public CrawlerTask info(CrawlerTask crawlerTask){
        return  crawlerTaskMapper.selectById(crawlerTask.getId());
    }

    @Override
    public Page<CrawlerTask> queryPage(PageParam pageParam){
        Page<CrawlerTask> page = new Page<>(pageParam);
        page = crawlerTaskMapper.selectPage(page);
        return page;
    }
}