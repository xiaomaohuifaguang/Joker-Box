package com.cat.simple.task;

import com.cat.common.entity.CONSTANTS;
import com.cat.common.entity.auth.RegisterUserInfo;
import com.cat.simple.config.redis.RedisService;
import com.cat.simple.service.UserService;
import freemarker.template.TemplateException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class UserRegisterTask {

    @Resource
    private UserService userService;
    @Resource
    private RedisService redisService;


    @PostConstruct
    void init() throws TemplateException, MessagingException, IOException {
//        log.info("UserRegisterTask init");
//
//        RegisterUserInfo registerUserInfo = new RegisterUserInfo();
//        registerUserInfo.setUsername("");
//        registerUserInfo.setPassword("");
//        registerUserInfo.setNickname("");
//        registerUserInfo.setMail("");
//        userService.code(registerUserInfo.getMail());
//        registerUserInfo.setCode(redisService.get(CONSTANTS.REDIS_PARENT_MAIL_CODE + registerUserInfo.getMail(), String.class));
//        userService.register(registerUserInfo);
    }




}
