package com.cat.simple.config.flowable.hook.context;

import com.cat.common.entity.process.ProcessHandleParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 启动流程操作上下文，封装启动请求参数。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StartContext{
    private ProcessHandleParam processHandleParam;

}
