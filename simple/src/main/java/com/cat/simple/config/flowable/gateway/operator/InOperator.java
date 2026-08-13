package com.cat.simple.config.flowable.gateway.operator;

import java.util.List;

/**
 * IN（包含）：actualValue 的任一元素与候选集中任一项语义相等即满足。
 * 支持 单值/集合 × 单值/集合 的任意组合，覆盖多选字段（List vs List）场景。
 */
public class InOperator implements ConditionOperator {

    @Override
    public boolean compare(Object actualValue, Object expectedValue) {
        if (actualValue == null || expectedValue == null) {
            return false;
        }

        List<Object> candidates = CompareSupport.parseCandidates(expectedValue);
        if (candidates.isEmpty()) {
            return false;
        }

        // actualValue 同样归一化为 List：单值 → 单元素列表，多选值 → 逐元素匹配
        List<Object> actualValues = CompareSupport.parseCandidates(actualValue);
        for (Object actual : actualValues) {
            for (Object candidate : candidates) {
                if (CompareSupport.semanticEquals(actual, candidate)) {
                    return true;
                }
            }
        }
        return false;
    }
}
