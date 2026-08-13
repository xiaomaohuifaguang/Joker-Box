package com.cat.simple.config.flowable.gateway.operator;

import java.util.List;

/**
 * NOT_IN（不包含）：actualValue 的所有元素都不在候选集中才满足，与 InOperator 严格互逆。
 * 支持 单值/集合 × 单值/集合 的任意组合，覆盖多选字段（List vs List）场景。
 */
public class NotInOperator implements ConditionOperator {

    @Override
    public boolean compare(Object actualValue, Object expectedValue) {
        // null 语义与 InOperator 保持一致
        if (actualValue == null || expectedValue == null) {
            return false;
        }

        List<Object> candidates = CompareSupport.parseCandidates(expectedValue);
        if (candidates.isEmpty()) {
            return true;
        }

        List<Object> actualValues = CompareSupport.parseCandidates(actualValue);
        for (Object actual : actualValues) {
            for (Object candidate : candidates) {
                if (CompareSupport.semanticEquals(actual, candidate)) {
                    return false; // 任一元素在集合中 → NotIn 为 false
                }
            }
        }
        return true;
    }
}
