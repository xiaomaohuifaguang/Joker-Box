package com.cat.simple.config.flowable.hook;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * 启动流程操作上下文，封装启动请求参数。
 */
@Data
@AllArgsConstructor
public class StartContext {
    private Integer processDefinitionId;
    private String title;
    private String applicantId;
    private Map<String, Object> initialVariables;
    private Map<String, Object> nodeFormData;
    private Map<String, Object> globalFormData;
}
