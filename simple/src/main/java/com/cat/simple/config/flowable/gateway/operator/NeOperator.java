package com.cat.simple.config.flowable.gateway.operator;

public class NeOperator implements ConditionOperator {
    @Override
    public boolean compare(Object actualValue, String expectedValue) {
        if (actualValue == null) return expectedValue != null;
        return !String.valueOf(actualValue).equals(expectedValue);
    }
}
