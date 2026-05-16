package com.cat.simple.config.flowable.hook;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class StartContext {
    private Integer processDefinitionId;
    private String title;
    private String applicantId;
    private Map<String, Object> initialVariables;
}
