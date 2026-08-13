package com.cat.simple.config.flowable.gateway.operator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexOperator implements ConditionOperator {

    /** 编译缓存上限，防止恶意/异常规则无限占用内存 */
    private static final int MAX_CACHE_SIZE = 256;
    private static final ConcurrentHashMap<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

    @Override
    public boolean compare(Object actualValue, Object expectedValue) {
        if (actualValue == null || expectedValue == null) {
            return false;
        }
        // 全量匹配语义（Pattern.matches）：正则需要描述整个值，而非子串
        return compile(String.valueOf(expectedValue)).matcher(String.valueOf(actualValue)).matches();
    }

    private Pattern compile(String regex) {
        try {
            Pattern cached = PATTERN_CACHE.get(regex);
            if (cached != null) {
                return cached;
            }
            Pattern compiled = Pattern.compile(regex);
            if (PATTERN_CACHE.size() < MAX_CACHE_SIZE) {
                PATTERN_CACHE.putIfAbsent(regex, compiled);
            }
            return compiled;
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException(
                    String.format("RegexOperator: 非法正则表达式 [%s]", regex), e);
        }
    }
}
