# 动态表单字段联动 — 后端修改反馈

> 日期：2026-05-21
> 针对前端反馈 `docs/dynamic-form-linkage-todo.md` 逐项回复

---

## 一、后端已修复的问题

### 1. EQ 比较方式 — 已改为字符串比较 ✅

**修改文件**：`common/src/main/java/com/cat/common/utils/dynamicForm/LinkageValidator.java`

**修改内容**：

```java
// 修改前
 case "EQ"        -> Objects.equals(actual, expect);
 case "NE"        -> !Objects.equals(actual, expect);

// 修改后
 case "EQ"        -> Objects.equals(
         actual == null ? "" : actual.toString(),
         expect == null ? "" : expect.toString());
 case "NE"        -> !Objects.equals(
         actual == null ? "" : actual.toString(),
         expect == null ? "" : expect.toString());
```

**效果**：`Objects.equals(123, "123")` 现在变成 `"123".equals("123")` → true，前后端一致。

---

### 2. IN 运算符数组处理 — 已改为展开判断 ✅

**修改文件**：`LinkageValidator.java`

**修改内容**：当 `actual` 为数组/集合类型时，判断数组中**任一元素**是否在 `expect` 中。

```java
 case "IN"        -> {
     // actual 为数组/集合时：判断任一元素是否在 expect 中
     if (actual instanceof Collection<?> ac) {
         if (expect instanceof Collection<?> ec) yield ac.stream().anyMatch(ec::contains);
         if (expect instanceof Object[] earr) {
             List<?> elist = Arrays.asList(earr);
             yield ac.stream().anyMatch(elist::contains);
         }
         yield ac.stream().anyMatch(e -> Objects.equals(e, expect));
     }
     if (actual instanceof Object[] aarr) {
         List<?> alist = Arrays.asList(aarr);
         if (expect instanceof Collection<?> ec) yield alist.stream().anyMatch(ec::contains);
         if (expect instanceof Object[] earr) {
             List<?> elist = Arrays.asList(earr);
             yield alist.stream().anyMatch(elist::contains);
         }
         yield alist.stream().anyMatch(e -> Objects.equals(e, expect));
     }
     if (expect instanceof Collection<?> c) yield c.contains(actual);
     if (expect instanceof Object[] arr) yield Arrays.asList(arr).contains(actual);
     yield Objects.equals(actual, expect);
 }
```

NOT_IN 同理改为 `noneMatch`。

**效果**：MULTISELECT 值为 `["java", "vue"]`，triggerValue 为 `["java", "python"]` 时，IN → true（"java" 在列表中）。

---

### 3. SET_SPAN actionValue 格式 — 已兼容裸数字 ✅

**修改文件**：`LinkageValidator.java`

**修改内容**：

```java
// 修改前
 case "SET_SPAN" -> {
     if (actionValue instanceof Map<?, ?> m) {
         Object v = m.get("span");
         if (v instanceof Number n) {
             effect.span = n.intValue();
         }
     }
 }

// 修改后
 case "SET_SPAN" -> {
     if (actionValue instanceof Number n) {
         effect.span = n.intValue();
     } else if (actionValue instanceof Map<?, ?> m) {
         Object v = m.get("span");
         if (v instanceof Number n) {
             effect.span = n.intValue();
         }
     }
 }
```

**效果**：前端提交裸数字 `12` 或对象 `{ "span": 12 }` 都能正确解析。

---

### 4. EMPTY 对空数组的处理 — 已统一为空 ✅

**修改文件**：`LinkageValidator.java`

**新增方法**：

```java
private static boolean isEmptyValue(Object value) {
    if (value == null) return true;
    if (value instanceof String s) return !StringUtils.hasText(s);
    if (value instanceof Collection<?> c) return c.isEmpty();
    if (value instanceof Object[] arr) return arr.length == 0;
    String str = value.toString();
    return !StringUtils.hasText(str);
}
```

EMPTY / NOT_EMPTY 改为调用此方法：

```java
 case "EMPTY"     -> isEmptyValue(actual);
 case "NOT_EMPTY" -> !isEmptyValue(actual);
```

**效果**：空数组 `[]` 现在被视为 EMPTY（true），符合用户直觉。

---

### 5. 节点为 null 的返回值 — 已改为 false ✅

**修改文件**：`LinkageValidator.java`

**修改内容**：

```java
// 修改前
private static boolean evaluateNode(DynamicFormLinkageNode node, Map<String, Object> data) {
    if (node == null) {
        return true;
    }

// 修改后
private static boolean evaluateNode(DynamicFormLinkageNode node, Map<String, Object> data) {
    if (node == null) {
        return false;
    }
```

**效果**：节点异常时不会意外触发联动动作。

---

### 6. 隐藏字段必填校验 — 后端已自动忽略 ✅

**说明**：后端 `validateFormData` 方法已正确处理 hidden / disabled 字段：

```java
// DynamicFormServiceImpl.java:659-672
Map<String, LinkageValidator.FieldEffect> effects = LinkageValidator.evalFieldEffects(rules, data);

for (DynamicFormField field : fields) {
    // ...
    LinkageValidator.FieldEffect effect = effects.get(field.getFieldId());
    boolean hidden = effect != null && !effect.visible();
    boolean disabled = effect != null && effect.disabled();

    if (hidden || disabled) {
        continue;  // 跳过 hidden/disabled 字段的所有校验
    }
    // ...
}
```

**效果**：hidden 或 disabled 字段不会触发 required、pattern、min/max 等任何校验。

---

## 二、需要前端修改的问题

### 1. [高] OPTION / VALUE 动作前端未实现

**后端不改**，这两个动作后端只保存配置、不参与运行时逻辑。

**前端需要**：

```js
// 在 FormMaker.vue 或 linkage.ts 中，联动求值后补充：

// OPTION：动态替换目标字段的 options
if (rule.actionType === 'OPTION' && triggered) {
    targetField.options = rule.actionValue
}

// VALUE：自动填充目标字段的值
if (rule.actionType === 'VALUE' && triggered) {
    formData[rule.targetFieldId] = rule.actionValue
}
```

**恢复逻辑建议**：
- **OPTION**：条件不满足时恢复字段原始 options（建议在字段配置中保存原始 options 的副本）
- **VALUE**：条件不满足时恢复 `defaultValue`（如果字段配置了默认值），否则清空

---

### 2. [低] REGEX 匹配方式

**后端不改**，保持 `Pattern.matches`（完全匹配）。

前端当前用 `new RegExp(regex).test(value)` 是**部分匹配**，建议改为完全匹配：

```js
// 方案A：给正则自动包 ^...$
const fullRegex = new RegExp(`^(?:${regex})$`)
return fullRegex.test(value)

// 方案B：test 后比对匹配长度
const r = new RegExp(regex)
const match = r.exec(value)
return match !== null && match[0].length === value.length
```

或者，如果前端业务上就是需要部分匹配，那请保持一致即可（联动中的 REGEX 很少用于字段校验，更多是条件判断）。

---

## 三、已一致、无需修改的问题

| 问题 | 状态 |
|------|------|
| SET_PATTERN actionValue 格式 | 前后端都是 `{ pattern, patternTips }` 对象格式，已一致 |

---

## 四、后端修改汇总表

| 序号 | 问题 | 修改文件 | 修改类型 |
|------|------|---------|---------|
| 1 | EQ/NE 比较方式 | `LinkageValidator.java` | 字符串化后比较 |
| 2 | IN/NOT_IN 数组展开 | `LinkageValidator.java` | actual 为数组时判断任一/所有元素 |
| 3 | SET_SPAN 兼容裸数字 | `LinkageValidator.java` | 同时支持 Number 和 Map |
| 4 | EMPTY/NOT_EMPTY 空数组 | `LinkageValidator.java` | 新增 `isEmptyValue` 方法 |
| 5 | evaluateNode(null) | `LinkageValidator.java` | true → false |
| 6 | hidden 字段校验跳过 | `DynamicFormServiceImpl.java` | 已有逻辑，无需修改 |

---

## 五、前端修改清单

| 优先级 | 文件 | 修改内容 |
|--------|------|---------|
| **高** | `linkage.ts` / `FormMaker.vue` | 补充 `OPTION` / `VALUE` 动作处理 |
| **高** | `LinkageEditor.vue` | `SET_SPAN` 的 actionValue 已兼容裸数字，但建议统一为对象格式 `{ span: 12 }` 更规范 |
| 低 | `linkage.ts` | `REGEX` 改为完全匹配（可选，视业务需求） |
| 低 | `main/IndexView.vue` | 提交时过滤 `visible=false` 的字段（后端已自动忽略校验，但提交数据可更干净） |

---

> 如有疑问请随时反馈。