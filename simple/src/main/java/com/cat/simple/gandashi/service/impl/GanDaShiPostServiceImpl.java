package com.cat.simple.gandashi.service.impl;

import com.cat.common.entity.Page;
import com.cat.common.entity.auth.User;
import com.cat.common.entity.ganDaShi.GanDaShiPost;
import com.cat.common.entity.ganDaShi.GanDaShiPostPageParam;
import com.cat.simple.ai.service.LlmService;
import com.cat.simple.config.opensearch.OpensearchUtils;
import com.cat.simple.config.rocketmq.post.ganDaShi.GanDaShiVectorRocketMqProductor;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.gandashi.mapper.GanDaShiCommentMapper;
import com.cat.simple.gandashi.mapper.GanDaShiPostMapper;
import com.cat.simple.gandashi.service.GanDaShiPostService;
import com.cat.simple.system.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.search.SourceConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Slf4j
public class GanDaShiPostServiceImpl implements GanDaShiPostService {


    @Resource
    private GanDaShiPostMapper ganDaShiPostMapper;
    @Resource
    private GanDaShiCommentMapper ganDaShiCommentMapper;
    @Resource
    private UserService userService;
    @Resource
    private OpensearchUtils opensearchUtils;
    @Resource
    private LlmService llmService;
    @Resource
    private GanDaShiVectorRocketMqProductor ganDaShiVectorRocketMqProductor;

    @Override
    @Transactional
    public boolean add(GanDaShiPost ganDaShiPost) {

        if (!StringUtils.hasText(ganDaShiPost.getContent())) {
            return false;
        }
        if (!StringUtils.hasText(ganDaShiPost.getTitle())) {
            ganDaShiPost.setTitle("无标题");
        }
        if (StringUtils.hasText(ganDaShiPost.getText())) {
            ganDaShiPost.setDigest(ganDaShiPost.getText().substring(0, Math.min(ganDaShiPost.getText().length(), 100)));
        }

        ganDaShiPost.setId(null);
        ganDaShiPost.setCreateBy(Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId());
        ganDaShiPost.setCreateTime(LocalDateTime.now());
        ganDaShiPost.setDeleted("0");
        ganDaShiPost.setViewCount(0);
        int insert = ganDaShiPostMapper.insert(ganDaShiPost);


        boolean b = opensearchUtils.insertOrUpdate(GanDaShiPost.INDEX, String.valueOf(ganDaShiPost.getId()), ganDaShiPost);

        ganDaShiVectorRocketMqProductor.send(ganDaShiPost);


        return insert == 1;
    }

    @Override
    @Transactional
    public boolean delete(GanDaShiPost ganDaShiPost) {
        ganDaShiPost = ganDaShiPostMapper.selectById(ganDaShiPost.getId());
        ganDaShiPost.setDeleted("1");
        boolean b = opensearchUtils.update(GanDaShiPost.INDEX, String.valueOf(ganDaShiPost.getId()), ganDaShiPost, GanDaShiPost.class);
        int i = ganDaShiPostMapper.deleteById(ganDaShiPost);
        return i == 1;
    }

    @Override
    @Transactional
    public GanDaShiPost info(GanDaShiPost ganDaShiPost) {
        ganDaShiPost = ganDaShiPostMapper.selectById(ganDaShiPost.getId());
        Integer viewCount = ganDaShiPost.getViewCount();
        ganDaShiPost.setViewCount(++viewCount);
        ganDaShiPostMapper.updateById(ganDaShiPost);
        boolean b = opensearchUtils.update(GanDaShiPost.INDEX, String.valueOf(ganDaShiPost.getId()), ganDaShiPost, GanDaShiPost.class);
        return ganDaShiPost;
    }

    @Override
    public Page<GanDaShiPost> queryPage(GanDaShiPostPageParam pageParam) {
        Page<GanDaShiPost> page = new Page<>(pageParam);

        if (pageParam.getCurrent() * pageParam.getSize() > 10000) {

            if (StringUtils.hasText(pageParam.getCreateUsername())) {
                User userByUsername = userService.getUserByUsername(pageParam.getCreateUsername());
                if (Objects.nonNull(userByUsername)) {
                    pageParam.setUserId(String.valueOf(userByUsername.getId()));
                } else {
                    return page;
                }
            }
            page = ganDaShiPostMapper.selectPage(page, pageParam);
            return page;
        }

        page = opensearchUtils.searchPage(GanDaShiPost.INDEX, page, Query.of(q -> q.bool(b -> {
            // 必须满足：逻辑未删除
            b.filter(f -> f.term(t -> t.field("deleted").value(FieldValue.of("0"))));

            // 多字段模糊搜索
            if (pageParam.getSearch() != null && !pageParam.getSearch().isEmpty()) {
                b.must(m -> m.multiMatch(mm -> mm
                        .fields("title", "text")
                        .query(pageParam.getSearch())
                ));
            }

            // 用户ID精确过滤
            if (pageParam.getUserId() != null && !pageParam.getUserId().isEmpty()) {
                b.filter(f -> f.term(t -> t.field("createBy").value(FieldValue.of(pageParam.getUserId()))));
            }


            return b;
        })), SourceConfig.of(sour -> sour.filter(f -> f
                .includes("*")              // 其他字段都要
                .excludes("textEmbeddings") // 唯独排除向量字段
        )), GanDaShiPost.class, SortOptions.of(sort -> sort.field(f -> f.field("createTime").order(SortOrder.Desc))));

        return page;

    }
}