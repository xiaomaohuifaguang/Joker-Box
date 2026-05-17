package com.cat.simple.task;

import com.cat.common.entity.CONSTANTS;
import com.cat.common.entity.auth.RegisterUserInfo;
import com.cat.common.utils.who.WhoUtils;
import com.cat.simple.config.redis.RedisService;
import com.cat.simple.system.service.UserService;
import freemarker.template.TemplateException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Random;

@Component
@Slf4j
public class UserRegisterTask {

    @Resource
    private UserService userService;
    @Resource
    private RedisService redisService;


    @PostConstruct
    void init() throws TemplateException, MessagingException, IOException {
        log.info("UserRegisterTask init");

        for (int i = 0; i < 1; i++) {
            new Thread(()->{
                RegisterUserInfo registerUserInfo = new RegisterUserInfo();

                int sex = WhoUtils.RANDOM.nextInt(2);
                String randomName = WhoUtils.getRandomName(sex);
                String randomCount = WhoUtils.getRandomCount(randomName);
                String randomEmail = WhoUtils.getRandomEmail();

                registerUserInfo.setUsername(randomCount);
                registerUserInfo.setPassword(CONSTANTS.DEFAULT_PASSWORD);
                registerUserInfo.setNickname(randomName);
                registerUserInfo.setMail(randomEmail);
                registerUserInfo.setSex(sex == 1 ? "男" : "女");
                registerUserInfo.setPhone(WhoUtils.getRandomPhone());
//        userService.code(registerUserInfo.getMail());
//        registerUserInfo.setCode(redisService.get(CONSTANTS.REDIS_PARENT_MAIL_CODE + registerUserInfo.getMail(), String.class));
                userService.register(registerUserInfo, false);
            }).start();
        }

    }




}
