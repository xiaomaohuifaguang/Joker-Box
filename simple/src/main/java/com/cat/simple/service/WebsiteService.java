package com.cat.simple.service;

import com.cat.common.entity.Page;
import com.cat.common.entity.website.Website;
import com.cat.common.entity.website.WebsiteGroup;
import com.cat.common.entity.website.WebsitePageParam;

import java.util.List;

/***
 * 网站收藏业务层
 * @title WebsiteService
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/10/27
 **/
public interface WebsiteService {

    Page<Website> queryPage(WebsitePageParam pageParam);

    List<WebsiteGroup> groups();

    boolean add(Website website);

    boolean delete(Integer id);

    boolean update(Website website);

    Website info(Integer id);


}
