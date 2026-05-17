package com.cat.simple.mail.service.impl;

import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.mail.MailInfo;
import com.cat.simple.mail.mapper.MailInfoMapper;
import com.cat.simple.mail.service.MailInfoService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class MailInfoServiceImpl implements MailInfoService {


    @Resource
    private MailInfoMapper mailInfoMapper;

    @Override
    public boolean add(MailInfo mailInfo){
        return mailInfoMapper.insert(mailInfo) == 1;
    }

    @Override
    public boolean delete(MailInfo mailInfo){
            return mailInfoMapper.deleteById(mailInfo) == 1;
    }

    @Override
    public boolean update(MailInfo mailInfo){
        return mailInfoMapper.updateById(mailInfo) == 1;
    }

    @Override
    public MailInfo info(MailInfo mailInfo){
        return  mailInfoMapper.selectById(mailInfo.getId());
    }

    @Override
    public Page<MailInfo> queryPage(PageParam pageParam){
        Page<MailInfo> page = new Page<>(pageParam);
        page = mailInfoMapper.selectPage(page, pageParam);
        return page;
    }
}