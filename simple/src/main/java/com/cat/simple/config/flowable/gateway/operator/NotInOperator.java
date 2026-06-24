package com.cat.simple.config.flowable.gateway.operator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

public class NotInOperator implements ConditionOperator {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public boolean compare(Object actualValue, String expectedValue) {
        if (actualValue == null || expectedValue == null) return true;
        try {
            List<String> expectedList = MAPPER.readValue(expectedValue, new TypeReference<List<String>>() {});
            return !expectedList.contains(String.valueOf(actualValue));
        } catch (Exception e) {
            return true;
        }
    }
}
