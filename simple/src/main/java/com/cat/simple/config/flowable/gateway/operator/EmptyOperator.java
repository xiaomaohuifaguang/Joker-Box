package com.cat.simple.config.flowable.gateway.operator;

import org.springframework.util.CollectionUtils;
import java.util.Collection;

public class EmptyOperator implements ConditionOperator {
    @Override
    public boolean compare(Object actualValue, String expectedValue) {
        if (actualValue == null) return true;
        if (actualValue instanceof String) return ((String) actualValue).isEmpty();
        if (actualValue instanceof Collection) return CollectionUtils.isEmpty((Collection<?>) actualValue);
        return false;
    }
}
