package com.cat.simple.config.flowable.hook.context;

import com.cat.common.entity.process.BackConfig;
import com.cat.common.entity.process.ProcessHandleParam;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 驳回操作上下文，封装驳回请求参数。
 */
@Data
@AllArgsConstructor
public class BackContext {
    private ProcessHandleParam processHandleParam;
}
