package com.cat.simple.service;


import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.mail.MailInfo;

public interface MailInfoService {

    boolean add(MailInfo mailInfo);

    boolean delete(MailInfo mailInfo);

    boolean update(MailInfo mailInfo);

    MailInfo info(MailInfo mailInfo);

    Page<MailInfo> queryPage(PageParam pageParam);
}