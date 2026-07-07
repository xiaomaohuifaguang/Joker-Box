package com.cat.simple.ai.tools;

import org.springframework.ai.chat.model.ToolContext;

import java.util.function.BiFunction;

// 将泛型参数从 String 改为 WeatherRequest
public class WeatherTool implements BiFunction<WeatherRequest, ToolContext, String> {

    @Override
    public String apply(WeatherRequest request, ToolContext toolContext) {
        // 从对象中获取城市名称
        String city = request.getCity();
        return "It's always sunny in " + city + "!";
    }
}