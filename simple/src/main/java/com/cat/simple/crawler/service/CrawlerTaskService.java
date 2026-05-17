package com.cat.simple.crawler.service;


import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.crawler.CrawlerTask;

public interface CrawlerTaskService {

    boolean add(CrawlerTask crawlerTask);

    boolean delete(CrawlerTask crawlerTask);

    boolean update(CrawlerTask crawlerTask);

    CrawlerTask info(CrawlerTask crawlerTask);

    Page<CrawlerTask> queryPage(PageParam pageParam);
}