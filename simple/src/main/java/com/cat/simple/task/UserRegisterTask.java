package com.cat.simple.task;

import com.cat.common.entity.CONSTANTS;
import com.cat.common.entity.auth.RegisterUserInfo;
import com.cat.common.utils.who.WhoUtils;
import com.cat.simple.system.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Component
@Slf4j
public class UserRegisterTask {

    @Resource
    private UserService userService;


//    @PostConstruct
    @Scheduled(initialDelay = 10, fixedDelay = 365 * 24 * 60 * 60, timeUnit = TimeUnit.SECONDS)
    @SchedulerLock(name = "UserRegisterTask.init", lockAtMostFor = "30m")
    void init(){
        log.info("UserRegisterTask init");

        for (int i = 0; i < 1; i++) {
            new Thread(()->{
                RegisterUserInfo registerUserInfo = new RegisterUserInfo();

                int sex = WhoUtils.RANDOM.nextInt(2);
                String randomName = WhoUtils.getRandomName(sex);
                String randomAccount = WhoUtils.getRandomAccount(randomName);
                String randomEmail = WhoUtils.getRandomEmail();

                registerUserInfo.setUsername(randomAccount);
                registerUserInfo.setPassword(CONSTANTS.DEFAULT_PASSWORD);
                registerUserInfo.setNickname(randomName);
                registerUserInfo.setMail(randomEmail);
                registerUserInfo.setSex(sex == 1 ? "男" : "女");
                registerUserInfo.setPhone(WhoUtils.getRandomPhone());
                userService.register(registerUserInfo, false);
            }).start();
        }

    }




}
