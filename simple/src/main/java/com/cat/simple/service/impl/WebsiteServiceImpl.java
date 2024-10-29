package com.cat.simple.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.common.entity.Page;

import com.cat.common.entity.website.Website;
import com.cat.common.entity.website.WebsiteGroup;
import com.cat.common.entity.website.WebsitePageParam;
import com.cat.simple.mapper.WebsiteMapper;
import com.cat.simple.service.WebsiteService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WebsiteServiceImpl implements WebsiteService {


    @Resource
    private WebsiteMapper websiteMapper;


    @Override
    public Page<Website> queryPage(WebsitePageParam pageParam) {
        Page<Website> page = new Page<>(pageParam);
        page = websiteMapper.selectPage(page, pageParam);
        return page;
    }

    @Override
    public List<WebsiteGroup> groups() {
        List<WebsiteGroup> result = new ArrayList<>();
        LinkedHashMap<String, List<Website>> map = new LinkedHashMap<>();
        List<Website> list = websiteMapper.selectList(new QueryWrapper<>());
        list.forEach(w -> {
            String groupName = StringUtils.hasText(w.getGroupName()) ? w.getGroupName() : "默认";
            List<Website> tmp = map.get(groupName);
            if (tmp == null) {
                tmp = new ArrayList<>();
            }
            tmp.add(w);
            map.put(groupName, tmp);
        });
        for (Map.Entry<String, List<Website>> entry : map.entrySet()) {
            result.add(new WebsiteGroup(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    @Override
    public boolean add(Website website) {
        if(!website.verify()) return false;
        website.setId(null);
        website.setCreateTime(LocalDateTime.now());
        return websiteMapper.insert(website) == 1;
    }

    @Override
    public boolean delete(Integer id) {
        return websiteMapper.deleteById(id) == 1;
    }

    @Override
    public boolean update(Website website) {
        if(!website.verify()) return false;
        return websiteMapper.update(website, new LambdaUpdateWrapper<Website>()
                .set(Website::getTitle, website.getTitle())
                .set(Website::getUrl, website.getUrl())
                .set(Website::getGroupName, website.getGroupName())
                .set(Website::getDescription, website.getDescription())
                .set(Website::getUpdateTime, LocalDateTime.now())
                .eq(Website::getId, website.getId())) == 1;
    }

    @Override
    public Website info(Integer id) {
        return websiteMapper.selectById(id);
    }
}
