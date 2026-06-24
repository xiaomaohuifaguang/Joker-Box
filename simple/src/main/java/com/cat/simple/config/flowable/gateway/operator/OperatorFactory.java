package com.cat.simple.config.flowable.gateway.operator;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class OperatorFactory {
    private final Map<String, ConditionOperator> operators;

    public OperatorFactory() {
        this.operators = Map.ofEntries(
                Map.entry("EQ", new EqOperator()),
                Map.entry("NE", new NeOperator()),
                Map.entry("GT", new GtOperator()),
                Map.entry("LT", new LtOperator()),
                Map.entry("GE", new GeOperator()),
                Map.entry("LE", new LeOperator()),
                Map.entry("IN", new InOperator()),
                Map.entry("NOT_IN", new NotInOperator()),
                Map.entry("EMPTY", new EmptyOperator()),
                Map.entry("NOT_EMPTY", new NotEmptyOperator()),
                Map.entry("REGEX", new RegexOperator())
        );
    }

    public ConditionOperator get(String operator) {
        return operators.getOrDefault(operator, (a, e) -> false);
    }
}
