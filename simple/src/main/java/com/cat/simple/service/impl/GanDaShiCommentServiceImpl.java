package com.cat.simple.service.impl;

import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.auth.LoginUser;
import com.cat.common.entity.auth.User;
import com.cat.common.entity.ganDaShi.GanDaShiComment;
import com.cat.common.entity.ganDaShi.GanDaShiCommentPageParam;
import com.cat.common.entity.ganDaShi.GanDaShiPost;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.mapper.GanDaShiCommentMapper;
import com.cat.simple.mapper.GanDaShiPostMapper;
import com.cat.simple.mapper.UserMapper;
import com.cat.simple.service.GanDaShiCommentService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;

@Service
public class GanDaShiCommentServiceImpl implements GanDaShiCommentService {


    @Resource
    private GanDaShiCommentMapper ganDaShiCommentMapper;
    @Resource
    private GanDaShiPostMapper ganDaShiPostMapper;
    @Resource
    private UserMapper userMapper;


    @Override
    @Transactional
    public GanDaShiComment add(GanDaShiComment ganDaShiComment){
        if(!StringUtils.hasText(ganDaShiComment.getComment())){
            return null;
        }

        GanDaShiPost ganDaShiPost = ganDaShiPostMapper.selectById(ganDaShiComment.getPostId());
        if(Objects.isNull(ganDaShiPost)){
            return null;
        }

        GanDaShiComment replayComment = null;
        if(StringUtils.hasText(ganDaShiComment.getReplayId())){
            replayComment = ganDaShiCommentMapper.selectById(ganDaShiComment.getReplayId());
            if(Objects.isNull(replayComment)){
                return null;
            }
        }

        // 判断主评论 并 计算主评论 回复数
        if(StringUtils.hasText(ganDaShiComment.getParentId())){
            GanDaShiComment parentComment = ganDaShiCommentMapper.selectById(ganDaShiComment.getParentId());
            if(Objects.isNull(parentComment)){
                return null;
            }else {
                Integer replayCount = parentComment.getReplayCount();
                replayCount = Objects.isNull(replayCount) ? 1 : ++replayCount;
                parentComment.setReplayCount(replayCount);
                ganDaShiCommentMapper.updateById(parentComment);
            }
        }else {
            ganDaShiComment.setParentId(null);
            ganDaShiComment.setReplayCount(0);
        }


        LoginUser loginUser = SecurityUtils.getLoginUser();
        ganDaShiComment.setCreateBy(Objects.requireNonNull(loginUser).getUserId());
        ganDaShiComment.setCreateByName(Objects.requireNonNull(loginUser).getNickname());
        ganDaShiComment.setCreateTime(LocalDateTime.now());
        ganDaShiComment.setDeleted("0");

        if(ganDaShiCommentMapper.insert(ganDaShiComment) == 1){
            if(StringUtils.hasText(ganDaShiComment.getParentId()) && StringUtils.hasText(ganDaShiComment.getReplayId()) && !ganDaShiComment.getParentId().equals(ganDaShiComment.getReplayId())){
                assert replayComment != null;
                User user = userMapper.selectById(replayComment.getCreateBy());
                ganDaShiComment.setReplayName(user.getNickname());
            }
            return ganDaShiComment;

        }else {
            return null;
        }

    }

    @Override
    public boolean delete(GanDaShiComment ganDaShiComment){
            return ganDaShiCommentMapper.deleteById(ganDaShiComment) == 1;
    }

    @Override
    public boolean update(GanDaShiComment ganDaShiComment){
        return ganDaShiCommentMapper.updateById(ganDaShiComment) == 1;
    }

    @Override
    public GanDaShiComment info(GanDaShiComment ganDaShiComment){
        return  ganDaShiCommentMapper.selectById(ganDaShiComment.getId());
    }

    @Override
    public Page<GanDaShiComment> queryPage(GanDaShiCommentPageParam pageParam){
        Page<GanDaShiComment> page = new Page<>(pageParam);
        page.setRecords(new ArrayList<>());
        if(!StringUtils.hasText(pageParam.getPostId())){
            return page;
        }




        page = ganDaShiCommentMapper.selectPage(page, pageParam);
        return page;
    }
}