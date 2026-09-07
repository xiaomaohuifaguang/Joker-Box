package com.cat.simple.ai.tools.system;

import com.cat.simple.system.service.UserService;
import dev.langchain4j.invocation.InvocationParameters;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;


@Component
public class SystemTools {

    private static final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Map<DayOfWeek, String> WEEKDAY_CN = Map.of(
            DayOfWeek.MONDAY, "星期一", DayOfWeek.TUESDAY, "星期二",
            DayOfWeek.WEDNESDAY, "星期三", DayOfWeek.THURSDAY, "星期四",
            DayOfWeek.FRIDAY, "星期五", DayOfWeek.SATURDAY, "星期六",
            DayOfWeek.SUNDAY, "星期日"
    );




    @Resource
    private UserService userService;

    @dev.langchain4j.agent.tool.Tool("获取当前日期和时间。当用户询问'现在几点''今天几号''今天周几'等时间相关问题时调用此工具。")
    public String getCurrentTime() {

        ZonedDateTime zdt = ZonedDateTime.now();
        String weekday = WEEKDAY_CN.get(zdt.getDayOfWeek());
        return String.format("%s %s)", zdt.format(DATE_TIME_FMT), weekday);
    }



    @dev.langchain4j.agent.tool.Tool("获取当前用户信息。当用户询问'我是谁''你认识我吗''我的角色''我的机构'，注意仅能获取当前用户信息")
    public UserInfoVO getUserInfo(InvocationParameters parameters) {

        String userId = parameters.get("userId");
        if(!StringUtils.hasText(userId)){
            throw new IllegalStateException("工具上下文中缺少 userId");
        }
        return userService.getUserInfoVO(userId);

    }






}
