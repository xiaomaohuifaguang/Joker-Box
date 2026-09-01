package com.cat.simple.config.ai.spring;

import com.cat.simple.ai.tools.file.FileParseTools;
import com.cat.simple.ai.tools.system.SystemTools;
import com.cat.simple.ai.tools.weather.WeatherTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAiConfig {


    @Bean
    public ToolCallbackProvider weatherToolsProvider(WeatherTools weatherTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(weatherTools)   // ← 扫描所有 @Tool 方法
                .build();                    // ← 自动生成 ToolCallback 列表
    }


    @Bean
    public ToolCallbackProvider systemToolsProvider(SystemTools systemTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(systemTools)   // ← 扫描所有 @Tool 方法
                .build();                    // ← 自动生成 ToolCallback 列表
    }


    @Bean
    public ToolCallbackProvider fileParseToolsProvider(FileParseTools fileParseTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(fileParseTools)   // ← 扫描所有 @Tool 方法
                .build();                    // ← 自动生成 ToolCallback 列表
    }


}
