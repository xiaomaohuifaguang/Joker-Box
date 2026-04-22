package com.cat.simple.service;


import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.ganDaShi.GanDaShiPost;
import com.cat.common.entity.ganDaShi.GanDaShiPostPageParam;

public interface GanDaShiPostService {

    boolean add(GanDaShiPost ganDaShiPost);

    boolean delete(GanDaShiPost ganDaShiPost);

    GanDaShiPost info(GanDaShiPost ganDaShiPost);

    Page<GanDaShiPost> queryPage(GanDaShiPostPageParam pageParam);
}