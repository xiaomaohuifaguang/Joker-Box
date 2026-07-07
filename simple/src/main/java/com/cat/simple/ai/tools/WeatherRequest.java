package com.cat.simple.ai.tools;

public class WeatherRequest {
    public String city; // 字段名必须和下面工具定义里的 property 名字一致

    // 必须提供无参构造函数
    public WeatherRequest() {}

    // Getter 和 Setter (如果使用 Lombok 可以省略，但为了保险建议写上或确认 Lombok 生效)
    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
