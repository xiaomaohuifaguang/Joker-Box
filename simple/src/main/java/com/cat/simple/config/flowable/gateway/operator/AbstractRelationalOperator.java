package com.cat.simple.config.flowable.gateway.operator;

import java.math.BigDecimal;
import java.util.function.IntPredicate;

/**
 * 大小关系比较器（GT / GE / LT / LE）的公共基类。
 * 子类只需提供比较符号和对 compareTo 结果的断言。
 *
 * <p>比较顺序：</p>
 * <ol>
 *   <li>数值优先（含数字字符串）——避免 "10" 与 "9" 走字典序导致结果反转；</li>
 *   <li>同类型 Comparable 快速路径（LocalDateTime、非数字 String 等）；</li>
 *   <li>类型不兼容 → 明确抛异常，不静默吞掉。</li>
 * </ol>
 */
abstract class AbstractRelationalOperator implements ConditionOperator {

    private final String symbol;
    private final IntPredicate resultMatcher;

    protected AbstractRelationalOperator(String symbol, IntPredicate resultMatcher) {
        this.symbol = symbol;
        this.resultMatcher = resultMatcher;
    }

    @Override
    public boolean compare(Object actualValue, Object expectedValue) {
        // null 不参与大小比较
        if (actualValue == null || expectedValue == null) {
            return false;
        }

        // 1. 数值比较优先于 Comparable：两侧只要都能解析为数值（含数字字符串），统一按 BigDecimal 比较
        BigDecimal actualBd = CompareSupport.toBigDecimalSafely(actualValue);
        BigDecimal expectedBd = CompareSupport.toBigDecimalSafely(expectedValue);
        if (actualBd != null && expectedBd != null) {
            return resultMatcher.test(actualBd.compareTo(expectedBd));
        }

        // 2. 同类型 Comparable 快速路径（LocalDateTime、非数字 String 等）
        if (actualValue instanceof Comparable
                && actualValue.getClass().equals(expectedValue.getClass())) {
            @SuppressWarnings("unchecked")
            Comparable<Object> comparable = (Comparable<Object>) actualValue;
            return resultMatcher.test(comparable.compareTo(expectedValue));
        }

        // 3. 类型不兼容 → 明确失败
        throw new IllegalArgumentException(
                String.format("%s: 无法比较 %s(%s) %s %s(%s)",
                        getClass().getSimpleName(),
                        actualValue, actualValue.getClass().getSimpleName(),
                        symbol,
                        expectedValue, expectedValue.getClass().getSimpleName()));
    }
}
