package com.cat.simple.ai.tools.weather;

import dev.langchain4j.agent.tool.P;
import org.springframework.stereotype.Component;


// 将泛型参数从 String 改为 WeatherRequest
@Component
public class WeatherTools{

    @dev.langchain4j.agent.tool.Tool("获取指定城市的当前天气信息")
    public String getWeather(
             @P("天气查询请求") WeatherRequest request
    ) {
        return "暴风雨， " + request.getCity() + "!";
    }
}