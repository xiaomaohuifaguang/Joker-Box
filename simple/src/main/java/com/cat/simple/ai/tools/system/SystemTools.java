package com.cat.simple.ai.tools.system;

import com.cat.simple.system.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;


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

    @Tool(description = "获取当前日期和时间。当用户询问'现在几点''今天几号''今天周几'等时间相关问题时调用此工具。")
    public String getCurrentTime() {

        ZonedDateTime zdt = ZonedDateTime.now();
        String weekday = WEEKDAY_CN.get(zdt.getDayOfWeek());
        return String.format("%s %s)", zdt.format(DATE_TIME_FMT), weekday);
    }



    @Tool(description = "获取当前用户信息。当用户询问'我是谁''你认识我吗''我的角色''我的机构'，注意仅能获取当前用户信息")
    public UserInfoVO getUserInfo(ToolContext toolContext) {

        Object userId = toolContext.getContext().get("userId");
        if (Objects.isNull(userId)) {
            throw new IllegalStateException("工具上下文中缺少 userId");
        }
        return userService.getUserInfoVO(userId.toString());
    }


    @Tool(description = "获取其他用户信息基础信息。当用户询问'帮我查一下张三的联系方式''李四的邮箱''系统内有王五这个人吗'")
    public List<UserInfoVO> getOtherUserInfo(@ToolParam(required = true, description = "检索关键词") String search) {
        return userService.getUserInfoVOList(search);
    }



}
