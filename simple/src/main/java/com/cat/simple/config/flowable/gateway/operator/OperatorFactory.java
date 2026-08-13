package com.cat.simple.config.flowable.gateway.operator;

import org.springframework.stereotype.Component;

import java.util.Locale;
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

    /**
     * 按操作符编码获取比较器，编码做 trim + 大写归一化。
     * 未知操作符直接抛异常——配置错误应快速失败，而不是静默按 false 走错流程分支。
     */
    public ConditionOperator get(String operator) {
        if (operator == null || operator.isBlank()) {
            throw new IllegalArgumentException("条件操作符不能为空");
        }
        ConditionOperator conditionOperator = operators.get(operator.trim().toUpperCase(Locale.ROOT));
        if (conditionOperator == null) {
            throw new IllegalArgumentException("未知的条件操作符: " + operator);
        }
        return conditionOperator;
    }
}
