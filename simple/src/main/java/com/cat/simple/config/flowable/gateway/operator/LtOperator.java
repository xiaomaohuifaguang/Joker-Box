package com.cat.simple.config.flowable.gateway.operator;

public class LtOperator implements ConditionOperator {
    @Override
    public boolean compare(Object actualValue, String expectedValue) {
        if (actualValue == null || expectedValue == null) return false;
        try {
            double actual = Double.parseDouble(String.valueOf(actualValue));
            double expected = Double.parseDouble(expectedValue);
            return actual < expected;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
