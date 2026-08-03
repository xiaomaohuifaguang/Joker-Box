package com.cat.simple.config.flowable.hook.context;

import com.cat.common.entity.process.ProcessHandleParam;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 拒绝操作上下文，封装拒绝请求参数。
 */
@Data
@AllArgsConstructor
public class RejectContext {
    private ProcessHandleParam processHandleParam;
}
