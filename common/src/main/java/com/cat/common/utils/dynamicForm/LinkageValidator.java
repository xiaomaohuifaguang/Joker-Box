package com.cat.common.utils.dynamicForm;

import com.cat.common.entity.dynamicForm.DynamicFormLinkageNode;
import com.cat.common.entity.dynamicForm.DynamicFormLinkageRule;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 字段联动规则执行器（支持树形嵌套条件：AND / OR / CONDITION）。
 * 根据表单数据和联动规则，计算每个字段的最终显隐/必填/禁用状态。
 */
public class LinkageValidator {

    private LinkageValidator() {}

    /**
     * 字段运行时状态。
     */
    public record FieldState(boolean visible, boolean required, boolean disabled) {}

    /**
     * 字段联动效果（含属性覆盖）。
     */
    public record FieldEffect(
            boolean visible,
            boolean required,
            boolean disabled,
            String pattern,
            String patternTips,
            Integer span
    ) {}

    /**
     * 根据联动规则计算所有字段状态。
     *
     * @param rules 联动规则（已按 sortOrder 排序，conditionTree 已组装为树）
     * @param data  表单数据
     * @return fieldId → FieldState
     */
    public static Map<String, FieldState> evalFieldStates(List<DynamicFormLinkageRule> rules, Map<String, Object> data) {
        Map<String, FieldEffect> effects = evalFieldEffects(rules, data);
        Map<String, FieldState> result = new HashMap<>();
        for (Map.Entry<String, FieldEffect> e : effects.entrySet()) {
            FieldEffect f = e.getValue();
            result.put(e.getKey(), new FieldState(f.visible(), f.required(), f.disabled()));
        }
        return result;
    }

    /**
     * 根据联动规则计算所有字段效果（含属性覆盖）。
     *
     * @param rules 联动规则（已按 sortOrder 排序，conditionTree 已组装为树）
     * @param data  表单数据
     * @return fieldId → FieldEffect
     */
    public static Map<String, FieldEffect> evalFieldEffects(List<DynamicFormLinkageRule> rules, Map<String, Object> data) {
        Map<String, MutableEffect> effects = new HashMap<>();

        if (CollectionUtils.isEmpty(rules) || data == null) {
            return Collections.emptyMap();
        }

        for (DynamicFormLinkageRule rule : rules) {
            if (Boolean.FALSE.equals(rule.getEnable())) {
                continue;
            }
            if (CollectionUtils.isEmpty(rule.getConditionTree())) {
                continue;
            }
            // 根节点为 conditionTree 的第一个元素（AND / OR）
            DynamicFormLinkageNode root = rule.getConditionTree().get(0);
            if (evaluateNode(root, data)) {
                MutableEffect effect = effects.computeIfAbsent(rule.getTargetFieldId(), k -> new MutableEffect());
                applyAction(effect, rule.getActionType(), rule.getActionValue());
            }
        }

        Map<String, FieldEffect> result = new HashMap<>();
        for (Map.Entry<String, MutableEffect> e : effects.entrySet()) {
            MutableEffect eff = e.getValue();
            result.put(e.getKey(), new FieldEffect(eff.visible, eff.required, eff.disabled,
                    eff.pattern, eff.patternTips, eff.span));
        }
        return result;
    }

    /**
     * 返回被隐藏的字段 fieldId 集合。
     */
    public static Set<String> hiddenFields(List<DynamicFormLinkageRule> rules, Map<String, Object> data) {
        Set<String> hidden = new HashSet<>();
        Map<String, FieldState> states = evalFieldStates(rules, data);
        for (Map.Entry<String, FieldState> e : states.entrySet()) {
            if (!e.getValue().visible()) {
                hidden.add(e.getKey());
            }
        }
        return hidden;
    }

    /**
     * 返回被禁用的字段 fieldId 集合。
     */
    public static Set<String> disabledFields(List<DynamicFormLinkageRule> rules, Map<String, Object> data) {
        Set<String> disabled = new HashSet<>();
        Map<String, FieldState> states = evalFieldStates(rules, data);
        for (Map.Entry<String, FieldState> e : states.entrySet()) {
            if (e.getValue().disabled()) {
                disabled.add(e.getKey());
            }
        }
        return disabled;
    }

    // ---------- private ----------

    /**
     * 递归判定节点树。
     */
    private static boolean evaluateNode(DynamicFormLinkageNode node, Map<String, Object> data) {
        if (node == null) {
            return true;
        }
        // 叶子条件节点
        if ("CONDITION".equals(node.getNodeType())) {
            return match(data.get(node.getTriggerFieldId()), node.getTriggerCondition(), node.getTriggerValue());
        }

        // 逻辑节点（AND / OR）
        List<DynamicFormLinkageNode> children = node.getChildren();
        if (CollectionUtils.isEmpty(children)) {
            return true;
        }
        boolean isAnd = "AND".equals(node.getNodeType());
        for (DynamicFormLinkageNode child : children) {
            boolean childResult = evaluateNode(child, data);
            if (isAnd && !childResult) {
                return false; // AND 短路
            }
            if (!isAnd && childResult) {
                return true;  // OR 短路
            }
        }
        return isAnd; // AND: 全通过返回 true；OR: 全不通过返回 false
    }

    private static void applyAction(MutableEffect effect, String actionType, Object actionValue) {
        switch (actionType) {
            case "SHOW"   -> effect.visible = true;
            case "HIDE"   -> effect.visible = false;
            case "REQUIRED" -> {
                boolean required = actionValue == null || !Boolean.FALSE.equals(actionValue);
                effect.required = required;
            }
            case "DISABLED" -> {
                boolean disabled = actionValue == null || !Boolean.FALSE.equals(actionValue);
                effect.disabled = disabled;
            }
            case "ENABLED"  -> effect.disabled = false;
            case "SET_PATTERN" -> {
                if (actionValue instanceof Map<?, ?> m) {
                    effect.pattern = (String) m.get("pattern");
                    effect.patternTips = (String) m.get("patternTips");
                }
            }
            case "SET_SPAN" -> {
                if (actionValue instanceof Map<?, ?> m) {
                    Object v = m.get("span");
                    if (v instanceof Number n) {
                        effect.span = n.intValue();
                    }
                }
            }
            default       -> { /* ignore unknown */ }
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean match(Object actual, String condition, Object expect) {
        String strActual = actual == null ? "" : actual.toString();

        return switch (condition) {
            case "EQ"        -> Objects.equals(actual, expect);
            case "NE"        -> !Objects.equals(actual, expect);
            case "EMPTY"     -> !StringUtils.hasText(strActual);
            case "NOT_EMPTY" -> StringUtils.hasText(strActual);
            case "GT", "LT", "GE", "LE" -> compareNumber(actual, condition, expect);
            case "IN"        -> {
                if (expect instanceof Collection<?> c) yield c.contains(actual);
                if (expect instanceof Object[] arr) yield Arrays.asList(arr).contains(actual);
                yield Objects.equals(actual, expect);
            }
            case "NOT_IN"    -> {
                if (expect instanceof Collection<?> c) yield !c.contains(actual);
                if (expect instanceof Object[] arr) yield !Arrays.asList(arr).contains(actual);
                yield !Objects.equals(actual, expect);
            }
            case "REGEX"     -> {
                if (!StringUtils.hasText(strActual) || expect == null) yield false;
                try {
                    yield Pattern.matches(expect.toString(), strActual);
                } catch (PatternSyntaxException e) {
                    yield false;
                }
            }
            default          -> false;
        };
    }

    private static boolean compareNumber(Object actual, String condition, Object expect) {
        if (actual == null || expect == null) return false;
        try {
            double a = Double.parseDouble(actual.toString());
            double e = Double.parseDouble(expect.toString());
            return switch (condition) {
                case "GT" -> a > e;
                case "LT" -> a < e;
                case "GE" -> a >= e;
                case "LE" -> a <= e;
                default   -> false;
            };
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static class MutableEffect {
        boolean visible = true;
        boolean required = false;
        boolean disabled = false;
        String pattern;
        String patternTips;
        Integer span;
    }
}
