package com.cat.simple.ai.tools.weather;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;


// 将泛型参数从 String 改为 WeatherRequest
@Component
public class WeatherTools{

    @Tool(description = "获取指定城市的当前天气信息")
    public String getWeather(
            @ToolParam(description = "天气查询请求") WeatherRequest request
    ) {
        return "It's always sunny in " + request.getCity() + "!";
    }
}