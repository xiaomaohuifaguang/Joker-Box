package com.cat.simple.ai.tools.weather;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "WeatherRequest", description = "天气请求")
public class WeatherRequest {

    @Schema(description = "城市名称，支持中文和拼音，如北京、beijing")
    public String city; // 字段名必须和下面工具定义里的 property 名字一致

    // 必须提供无参构造函数
    public WeatherRequest() {}


}
