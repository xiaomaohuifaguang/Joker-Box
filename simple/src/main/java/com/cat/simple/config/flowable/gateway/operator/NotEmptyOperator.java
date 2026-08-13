package com.cat.simple.config.flowable.gateway.operator;

public class NotEmptyOperator implements ConditionOperator {

    @Override
    public boolean compare(Object actualValue, Object expectedValue) {
        // 与 EmptyOperator 严格互逆
        return !CompareSupport.isEmptyValue(actualValue);
    }
}
