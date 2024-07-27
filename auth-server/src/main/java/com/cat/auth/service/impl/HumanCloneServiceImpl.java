package com.cat.auth.service.impl;

import cn.hutool.extra.pinyin.PinyinUtil;
import com.cat.auth.service.HumanCloneService;
import com.cat.common.entity.HumanClone;
import com.cat.common.entity.RegisterUserInfo;
import com.cat.common.utils.who.WhoUtils;
import org.springframework.stereotype.Service;

/***
 * 人类克隆其业务层实现
 * @title HumanCloneServiceImpl
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/10 17:00
 **/
@Service
public class HumanCloneServiceImpl implements HumanCloneService {


    @Override
    public HumanClone make() {
        HumanClone humanClone = new HumanClone();
        int sex = WhoUtils.RANDOM.nextInt(2);
        String randomName = WhoUtils.getRandomName(sex);
        humanClone.setUsername(PinyinUtil.getPinyin(randomName,""));
        humanClone.setNickname(randomName);
        humanClone.setMail(WhoUtils.getRandomEmail());
        humanClone.setPhone(WhoUtils.getRandomPhone());
        humanClone.setSex(sex==1 ? "男" : "女");
        return humanClone;
    }
}
