package com.cat.simple.config.flowable.hook.context;

import com.cat.common.entity.process.ProcessHandleParam;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 认领操作上下文，封装认领请求参数。
 */
@Data
@AllArgsConstructor
public class ClaimContext {
    private ProcessHandleParam processHandleParam;
}
