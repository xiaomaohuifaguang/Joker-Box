package com.cat.simple.service.impl;

import com.cat.common.entity.Page;
import com.cat.common.entity.auth.User;
import com.cat.common.entity.ganDaShi.GanDaShiPost;
import com.cat.common.entity.ganDaShi.GanDaShiPostPageParam;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.mapper.GanDaShiCommentMapper;
import com.cat.simple.mapper.GanDaShiPostMapper;
import com.cat.simple.service.GanDaShiPostService;
import com.cat.simple.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class GanDaShiPostServiceImpl implements GanDaShiPostService {


    @Resource
    private GanDaShiPostMapper ganDaShiPostMapper;
    @Resource
    private GanDaShiCommentMapper ganDaShiCommentMapper;
    @Resource
    private UserService userService;

    @Override
    public boolean add(GanDaShiPost ganDaShiPost){

        if(!StringUtils.hasText(ganDaShiPost.getContent())){
            return false;
        }
        if(!StringUtils.hasText(ganDaShiPost.getTitle())){
            ganDaShiPost.setTitle("无标题");
        }
        if(StringUtils.hasText(ganDaShiPost.getText())){
            ganDaShiPost.setDigest(ganDaShiPost.getText().substring(0,Math.min(ganDaShiPost.getText().length(), 100)));
        }


        ganDaShiPost.setId(null);
        ganDaShiPost.setCreateBy(Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId());
        ganDaShiPost.setCreateTime(LocalDateTime.now());
        ganDaShiPost.setViewCount(0);
        return ganDaShiPostMapper.insert(ganDaShiPost) == 1;
    }

    @Override
    public boolean delete(GanDaShiPost ganDaShiPost){
            return ganDaShiPostMapper.deleteById(ganDaShiPost) == 1;
    }

    @Override
    @Transactional
    public GanDaShiPost info(GanDaShiPost ganDaShiPost){
        ganDaShiPost = ganDaShiPostMapper.selectById(ganDaShiPost.getId());
        Integer viewCount = ganDaShiPost.getViewCount();
        ganDaShiPost.setViewCount(++viewCount);
        ganDaShiPostMapper.updateById(ganDaShiPost);
        return ganDaShiPost;
    }

    @Override
    public Page<GanDaShiPost> queryPage(GanDaShiPostPageParam pageParam){
        Page<GanDaShiPost> page = new Page<>(pageParam);
        if(StringUtils.hasText(pageParam.getCreateUsername())){
            User userByUsername = userService.getUserByUsername(pageParam.getCreateUsername());
            if(Objects.nonNull(userByUsername)){
                pageParam.setUserId(String.valueOf(userByUsername.getId()));
            }else {
                return page;
            }
        }
        page = ganDaShiPostMapper.selectPage(page, pageParam);
        return page;
    }
}