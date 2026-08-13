package com.cat.simple.config.flowable.gateway.operator;

import java.util.Objects;

public class EqOperator implements ConditionOperator {

    @Override
    public boolean compare(Object actualValue, Object expectedValue) {
        // null 安全处理
        if (actualValue == null || expectedValue == null) {
            return Objects.equals(actualValue, expectedValue);
        }
        // 语义相等比较（数值跨类型按 BigDecimal 比较，BigDecimal("1.0") 与 BigDecimal("1") 视为相等）
        return CompareSupport.semanticEquals(actualValue, expectedValue);
    }
}
