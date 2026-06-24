package com.cat.simple.config.flowable.gateway.operator;

public class RegexOperator implements ConditionOperator {
    @Override
    public boolean compare(Object actualValue, String expectedValue) {
        if (actualValue == null || expectedValue == null) return false;
        return String.valueOf(actualValue).matches(expectedValue);
    }
}
