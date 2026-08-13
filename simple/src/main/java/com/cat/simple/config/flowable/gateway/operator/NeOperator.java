package com.cat.simple.config.flowable.gateway.operator;

public class NeOperator implements ConditionOperator {

    @Override
    public boolean compare(Object actualValue, Object expectedValue) {
        // null 语义：仅当一侧为null时视为"不等"；两侧都null视为"相等"→返回false
        if (actualValue == null && expectedValue == null) {
            return false;
        }
        if (actualValue == null || expectedValue == null) {
            return true;
        }
        // 与 EqOperator 严格互逆
        return !CompareSupport.semanticEquals(actualValue, expectedValue);
    }
}
