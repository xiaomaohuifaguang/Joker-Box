package com.cat.simple.config.flowable.hook.context;

import com.cat.common.entity.process.ProcessHandleParam;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * 通过操作上下文，封装通过请求参数。
 */
@Data
@AllArgsConstructor
public class PassContext {
    private ProcessHandleParam processHandleParam;
}
