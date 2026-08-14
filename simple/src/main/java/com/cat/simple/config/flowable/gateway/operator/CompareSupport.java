package com.cat.simple.config.flowable.gateway.operator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 条件比较器的公共比较内核。
 * 语义相等、数值转换、候选集解析、空值判断统一收口在这里，
 * 保证 EQ / NE / IN / NOT_IN 的"相等"语义、EMPTY / NOT_EMPTY 的"空"语义严格互逆。
 */
final class CompareSupport {

    private static final Logger log = LoggerFactory.getLogger(CompareSupport.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CompareSupport() {
    }

    /**
     * 语义相等比较：
     * 1. Objects.equals（同类型 + null 安全）
     * 2. 数值跨类型按 BigDecimal 比较（解决 1 vs 1.0、int vs String 数字）
     * 3. 布尔忽略大小写
     * 4. String.valueOf 兜底
     */
    static boolean semanticEquals(Object a, Object b) {
        if (Objects.equals(a, b)) {
            return true;
        }

        BigDecimal ba = toBigDecimalSafely(a);
        BigDecimal bb = toBigDecimalSafely(b);
        if (ba != null && bb != null) {
            return ba.compareTo(bb) == 0;
        }

        if (isBooleanLike(a) && isBooleanLike(b)) {
            return Boolean.parseBoolean(String.valueOf(a))
                    == Boolean.parseBoolean(String.valueOf(b));
        }

        return String.valueOf(a).equals(String.valueOf(b));
    }

    /**
     * 安全转换为 BigDecimal：Number / 数字字符串可转，其余返回 null。
     * 同时承担"是否数值"的判断职责（返回非 null 即数值）。
     */
    static BigDecimal toBigDecimalSafely(Object obj) {
        if (obj instanceof BigDecimal) {
            return (BigDecimal) obj;
        }
        if (obj instanceof Number) {
            return new BigDecimal(obj.toString());
        }
        if (obj instanceof String) {
            try {
                return new BigDecimal(((String) obj).trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    static boolean isBooleanLike(Object obj) {
        if (obj instanceof Boolean) {
            return true;
        }
        if (obj instanceof String) {
            String s = (String) obj;
            return "true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s);
        }
        return false;
    }

    /**
     * 将多种格式的期望值统一解析为候选列表。
     * 支持: Collection / 数组 / JSON数组字符串 / 逗号分隔字符串 / 单值包装。
     * 注意：逗号分隔格式不支持值本身含逗号的场景，含特殊字符请使用 JSON 数组格式。
     */
    static List<Object> parseCandidates(Object expectedValue) {
        if (expectedValue instanceof Collection) {
            return new ArrayList<>((Collection<?>) expectedValue);
        }
        if (expectedValue.getClass().isArray()) {
            int len = Array.getLength(expectedValue);
            List<Object> list = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                list.add(Array.get(expectedValue, i));
            }
            return list;
        }
        if (expectedValue instanceof String) {
            String str = ((String) expectedValue).trim();
            if (str.startsWith("[")) {
                try {
                    return MAPPER.readValue(str, new TypeReference<List<Object>>() {
                    });
                } catch (Exception e) {
                    // JSON 解析失败不做逗号分隔降级（避免产出 "[1"、"2]" 之类的垃圾候选），
                    // 记日志后按整串单候选处理，此时条件不匹配、语义明确
                    log.warn("条件候选值 JSON 解析失败，按单值处理: [{}], 原因: {}", str, e.getMessage());
                    return Collections.singletonList(str);
                }
            }
            if (!str.isEmpty()) {
                String[] parts = str.split(",");
                List<Object> list = new ArrayList<>(parts.length);
                for (String part : parts) {
                    list.add(part.trim());
                }
                return list;
            }
            return Collections.emptyList();
        }
        return Collections.singletonList(expectedValue);
    }

    /**
     * 空值判断：null、空字符串、空集合、空 Map、空 Optional、空数组均视为空。
     * 注意：空白字符串（" "）不算空，与 String.isEmpty 语义一致。
     */
    static boolean isEmptyValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof CharSequence) {
            return ((CharSequence) value).isEmpty();
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).isEmpty();
        }
        if (value instanceof Map) {
            return ((Map<?, ?>) value).isEmpty();
        }
        if (value instanceof Optional) {
            return ((Optional<?>) value).isEmpty();
        }
        if (value.getClass().isArray()) {
            return Array.getLength(value) == 0;
        }
        // 其他类型（Integer、Boolean、自定义对象等）有值即非空
        return false;
    }
}
