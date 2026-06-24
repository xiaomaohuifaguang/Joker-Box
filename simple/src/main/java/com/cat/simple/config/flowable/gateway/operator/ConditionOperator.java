package com.cat.simple.config.flowable.gateway.operator;

public interface ConditionOperator {
    boolean compare(Object actualValue, String expectedValue);
}
