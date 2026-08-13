package com.cat.simple.config.flowable.gateway.operator;

public class EmptyOperator implements ConditionOperator {

    @Override
    public boolean compare(Object actualValue, Object expectedValue) {
        return CompareSupport.isEmptyValue(actualValue);
    }
}
