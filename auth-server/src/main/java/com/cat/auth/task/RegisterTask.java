package com.cat.auth.task;

import cn.hutool.extra.pinyin.PinyinUtil;
import com.cat.auth.config.redis.RedisService;
import com.cat.auth.service.UserService;
import com.cat.common.entity.CONSTANTS;
import com.cat.common.entity.DTO;
import com.cat.common.entity.RegisterUserInfo;
import com.cat.common.utils.who.WhoUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/***
 * <TODO description class purpose>
 * @title RegisterTask
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/10 1:18
 **/
@Service
@Slf4j
public class RegisterTask {

    @Resource
    private RedisService redisService;
    @Resource
    private UserService userService;


//    @Scheduled(fixedRate = 1000)
//    @PostConstruct
    void PersonMakePerson(){
        log.info("========================================");
        log.info("TASK->PersonMakePerson->人造人任务开始");
        int num = 0;
        while (num < 1){
            RegisterUserInfo registerUserInfo = new RegisterUserInfo();
            int sex = WhoUtils.RANDOM.nextInt(2);
            String randomName = WhoUtils.getRandomName(sex);
            registerUserInfo.setUsername(PinyinUtil.getPinyin(randomName,""));
            registerUserInfo.setPassword("12345678");
            registerUserInfo.setNickname(randomName);
            registerUserInfo.setMail(WhoUtils.getRandomEmail());
            registerUserInfo.setPhone(WhoUtils.getRandomPhone());
            registerUserInfo.setSex(sex==1 ? "男" : "女");
            String codeTmp = "123456";
            redisService.set(CONSTANTS.REDIS_PARENT_MAIL_CODE+registerUserInfo.getMail(),codeTmp,300);
            registerUserInfo.setCode(codeTmp);
            DTO<?> register = userService.register(registerUserInfo);
            log.info(registerUserInfo.toString());
            log.info(register.toString());
            num++;
        }
        log.info("TASK->PersonMakePerson->人造人任务结束");
    }




}
