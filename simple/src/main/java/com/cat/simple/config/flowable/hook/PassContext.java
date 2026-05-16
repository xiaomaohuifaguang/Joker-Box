package com.cat.simple.config.flowable.hook;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class PassContext {
    private Integer processInstanceId;
    private String taskId;
    private String remark;
    private Map<String, Object> formData;
}
