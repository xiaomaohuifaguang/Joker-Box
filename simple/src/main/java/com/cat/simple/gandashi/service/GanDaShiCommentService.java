package com.cat.simple.gandashi.service;


import com.cat.common.entity.Page;
import com.cat.common.entity.ganDaShi.GanDaShiComment;
import com.cat.common.entity.ganDaShi.GanDaShiCommentPageParam;

public interface GanDaShiCommentService {

    GanDaShiComment add(GanDaShiComment ganDaShiComment);

    boolean delete(GanDaShiComment ganDaShiComment);

    boolean update(GanDaShiComment ganDaShiComment);

    GanDaShiComment info(GanDaShiComment ganDaShiComment);

    Page<GanDaShiComment> queryPage(GanDaShiCommentPageParam pageParam);
}