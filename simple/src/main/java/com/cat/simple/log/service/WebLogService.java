package com.cat.simple.log.service;


import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.WebLog;

public interface WebLogService {

    boolean add(WebLog webLog);

    boolean delete(WebLog webLog);

    boolean update(WebLog webLog);

    WebLog info(WebLog webLog);

    Page<WebLog> queryPage(PageParam pageParam);
}