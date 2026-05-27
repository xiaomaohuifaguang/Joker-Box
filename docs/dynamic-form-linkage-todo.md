# 动态表单字段联动 — 前后端联调待确认事项

> 生成日期：2026-05-21
> 基于前端代码（`src/components/dynamicForm/linkage.ts`、`FormMaker.vue`、`LinkageEditor.vue`）与后端文档（`docs/dynamic-form-linkage-design.md`）比对整理

---

## 优先级说明

- **[高]** 功能阻塞或存在 Bug，必须确认/修复
- **[中]** 存在不一致风险，建议本轮确认
- **[低]** 细节差异，可延后处理

---

## 1. [高] OPTION / VALUE 动作前端未实现

### 问题描述

后端文档明确说明 `OPTION` 和 `VALUE` 两个动作**后端不处理，必须由前端实现**：

| 动作 | 后端支持 | 前端必须 |
|------|---------|---------|
| `OPTION` | **否** | **必须** |
| `VALUE` | **否** | **必须** |

但前端 `linkage.ts` 的 `computeFieldStates()` 函数中 `switch (rule.actionType)` **只处理了 7 个动作**，缺少 `OPTION` 和 `VALUE`：

```js
// linkage.ts:172-208 — 已处理的动作
SHOW | HIDE | REQUIRED | DISABLED | ENABLED | SET_PATTERN | SET_SPAN
// 缺少：OPTION | VALUE
```

### 影响

- 配置了 `OPTION` 动作的联动规则：目标字段的选项列表不会被动态替换
- 配置了 `VALUE` 动作的联动规则：目标字段不会被自动填充值
- 用户配置的联动规则在前端运行时不生效

### 待确认

1. 前端是否需要在 `computeFieldStates`（或 `FormMaker.vue`）中补充 `OPTION` / `VALUE` 的处理逻辑？
2. `OPTION` 动作在条件**不满足时是否需要恢复原始 options**？恢复时机和方式？
3. `VALUE` 动作在条件**不满足时是否需要恢复 defaultValue 或清空**？

### 建议前端实现方案

在 `FormMaker.vue` 的字段值变化监听中，联动求值后补充：

```js
// OPTION：动态替换目标字段的 options
if (rule.actionType === 'OPTION' && triggered) {
    // 找到目标字段，替换 options
    targetField.options = rule.actionValue
}

// VALUE：自动填充目标字段的值
if (rule.actionType === 'VALUE' && triggered) {
    formData[rule.targetFieldId] = rule.actionValue
}
```

---

## 2. [高] EQ 比较方式不一致

### 问题描述

| 来源 | EQ 比较逻辑 |
|------|------------|
| 后端文档（4.2节） | `Objects.equals(actual, expect)`，null 安全，**不转字符串** |
| 前端 `linkage.ts:37-48` | `String(fieldValue ?? '') === String(triggerValue ?? '')`，**全部转字符串后比较** |

### 差异场景

**场景1：数值比较**
```js
// 后端：Objects.equals(123, "123") → false（类型不同）
// 前端：String(123) === String("123") → "123" === "123" → true
```

**场景2：数组比较**
```js
// 后端：Objects.equals(["a","b"], ["a","b"]) → false（引用不同）
// 前端：数组逐一字符串比较 → true（如果元素相同）
```

**场景3：null 比较**
```js
// 后端：Objects.equals(null, null) → true
// 前端：String(null ?? '') === String(null ?? '') → '' === '' → true
```

### 待确认

1. 后端实际使用的是 `Objects.equals` 还是也有字符串转换逻辑？
2. 对于 NUMBER/SLIDER/RATE 等数值字段，EQ 是按原始类型比较还是按字符串比较？
3. 建议统一为**字符串比较**（前后端都做 toString 后比较），因为前端传来的值本身就是字符串化的

---

## 3. [高] IN 运算符数组处理不一致

### 问题描述

| 来源 | IN 运算符对数组的处理 |
|------|---------------------|
| 后端文档（4.2节） | **不会自动展开数组的每个元素逐一判断**。`collection.contains(actual)` 比较整个数组对象 |
| 前端 `linkage.ts:79-87` | **展开每个元素逐一判断**：`fieldValue.some(v => list.map(String).includes(String(v)))` |

### 差异场景

**场景：MULTISELECT 字段值为 `["java", "vue"]`，触发值为 `["java", "python"]`**

```js
// 后端逻辑：collection.contains(["java", "vue"]) → false（数组对象引用不同）
// 前端逻辑：["java", "vue"].some(v => ["java", "python"].includes(v)) → true（"java" 在列表中）
```

后端文档明确说明：
> "MULTISELECT / CHECKBOX：判断用户选中的**某一个值**是否在范围内（注意：不会自动展开数组的每个元素逐一判断）"

### 待确认

1. 后端 `IN` 对数组类型字段的实际行为是什么？是判断整个数组是否在列表中，还是判断数组中的某个元素？
2. 如果后端确实是**引用级比较**（不展开），前端需要改为一致逻辑
3. 建议：`IN` 对数组字段的行为应明确为：
   - 方案A：数组任一元素在 triggerValue 列表中（前端当前逻辑，更实用）
   - 方案B：整个数组对象等于 triggerValue 的某个元素（后端文档描述）

---

## 4. [高] SET_SPAN actionValue 格式不一致

### 问题描述

| 来源 | SET_SPAN actionValue 格式 |
|------|--------------------------|
| 后端文档（5.2节） | 必须是**对象格式**：`{ "span": 12 }` |
| 前端 `LinkageEditor.vue:91-93` | 绑定为**裸数字**：`v-model="element.actionValue"`（el-input-number） |
| 前端 `linkage.ts:202-207` | `Number(rule.actionValue)`，兼容裸数字 |

**前端提交的数据格式：**
```json
{
  "actionType": "SET_SPAN",
  "actionValue": 12   // 后端期望 { "span": 12 }
}
```

**后端期望的数据格式：**
```json
{
  "actionType": "SET_SPAN",
  "actionValue": { "span": 12 }
}
```

后端代码逻辑：
```java
if (actionValue instanceof Map<?, ?> m) {
    Object v = m.get("span");
    if (v instanceof Number n) {
        effect.span = n.intValue();
    }
}
```

后端只接受 `Map` 类型，裸数字 `12` 无法被解析。

### 待确认

1. 后端是否支持裸数字格式的 `actionValue`？
2. 若不支持，前端 `LinkageEditor.vue` 需要将 SET_SPAN 的 actionValue 改为对象格式 `{ span: 12 }`

---

## 5. [中] EMPTY 对空数组的处理不一致

### 问题描述

| 来源 | EMPTY 对空数组 `[]` 的处理 |
|------|---------------------------|
| 后端文档（4.2节） | **不为空**。空数组 toString 是 `"[]"`，`StringUtils.hasText("[]")` 为 true |
| 前端 `linkage.ts:24-29` | **为空**。`Array.isArray(v) ? v.length === 0` |

### 差异场景

```js
// MULTISELECT 字段值为 []
// 后端：EMPTY → false（不为空）
// 前端：EMPTY → true（为空）
```

### 待确认

1. 数组类型字段（MULTISELECT / CHECKBOX / MULTICASCADER / DATERANGE / UPLOAD）的 `EMPTY` 判断逻辑以谁为准？
2. 建议统一为：**空数组视为空**（更符合用户直觉）

---

## 6. [中] 节点为 null 的返回值不一致

### 问题描述

| 场景 | 后端文档（3.4节） | 前端 `linkage.ts:121` |
|------|------------------|----------------------|
| 节点为 null | 返回 **true**（视为满足） | 返回 **false** |

### 影响

如果某条规则的条件树根节点为 null（数据异常），后端会视为条件满足并执行动作，前端会视为不满足并跳过。

### 待确认

1. 哪种行为更合理？建议统一为 **false**（节点异常时不应触发动作）

---

## 7. [中] 隐藏字段未从提交数据中过滤

### 问题描述

后端文档（7.4节、10.4节）明确建议：
> "字段被隐藏（visible=false）时，前端不应将其值包含在提交数据中"
> "后端在校验时自动忽略 hidden 字段的 required 检查"

但前端 `main/IndexView.vue` 的 `convertSubmitData()` **没有过滤 hidden 字段**，所有字段的值都会提交。

### 待确认

1. 后端 `submit` 接口是否会自动忽略 hidden 字段的必填校验？
2. 前端是否需要在提交前过滤掉 `visible=false` 的字段？
3. 如果过滤，被隐藏字段的值是保留在 `formData` 中（仅提交时过滤）还是直接清空？

---

## 8. [低] REGEX 匹配方式不一致

### 问题描述

| 来源 | REGEX 匹配方式 |
|------|---------------|
| 后端文档（4.2节） | `Pattern.matches(expect.toString(), strActual)` —— **完全匹配**（自动加 `^...$`） |
| 前端 `linkage.ts:103-109` | `new RegExp(String(triggerValue)).test(String(fieldValue ?? ''))` —— **部分匹配** |

### 差异场景

```js
// triggerValue: "\d+"
// fieldValue: "abc123def"
// 后端：Pattern.matches("\d+", "abc123def") → false（不是完全匹配）
// 前端：/\d+/.test("abc123def") → true（部分匹配）
```

### 待确认

1. 后端实际使用的是 `Pattern.matches`（完全匹配）还是 `matcher.find()`（部分匹配）？
2. 建议统一为**完全匹配**，因为字段校验通常需要整值匹配

---

## 9. [低] SET_PATTERN actionValue 格式兼容

### 现状

| 来源 | SET_PATTERN actionValue 格式 |
|------|-----------------------------|
| 后端文档（5.2节） | 对象格式：`{ "pattern": "...", "patternTips": "..." }` |
| 前端 `LinkageEditor.vue:85-88` | 对象格式：`{ pattern: '', patternTips: '' }` ✅ |
| 前端 `linkage.ts:192-200` | 同时兼容对象和裸字符串 ✅ |

**结论：** SET_PATTERN 的前后端格式一致，无需修改。

---

## 前端修改清单（待确认后执行）

### 必须修复（高）

| 序号 | 文件 | 修改内容 |
|------|------|---------|
| 1 | `FormMaker.vue` / `linkage.ts` | 补充 `OPTION` / `VALUE` 动作处理逻辑 |
| 2 | `LinkageEditor.vue` | `SET_SPAN` 的 `actionValue` 改为对象格式 `{ span: 12 }` |

### 建议修复（中）

| 序号 | 文件 | 修改内容 |
|------|------|---------|
| 3 | `linkage.ts` | `EMPTY` 对空数组的处理与后端对齐 |
| 4 | `main/IndexView.vue` | `convertSubmitData` 过滤 `visible=false` 的字段 |
| 5 | `linkage.ts` | `EQ` / `IN` 运算符逻辑与后端对齐 |

### 可选优化（低）

| 序号 | 文件 | 修改内容 |
|------|------|---------|
| 6 | `linkage.ts` | `REGEX` 改为完全匹配（加 `^...$`） |
| 7 | `linkage.ts` | `evalConditionTree(null)` 返回值统一 |

---

## 附录：前端关键代码位置

| 功能 | 文件路径 |
|------|---------|
| 联动求值引擎 | `src/components/dynamicForm/linkage.ts` |
| 表单设计器（运行时） | `src/components/dynamicForm/FormMaker.vue` |
| 联动规则编辑器 | `src/components/dynamicForm/LinkageEditor.vue` |
| 条件树节点 | `src/components/dynamicForm/ConditionTreeNode.vue` |
| 用户填表提交 | `src/views/main/dynamicForm/IndexView.vue` |

---

> 建议优先处理 **[高]** 优先级事项（1-2项），特别是 `OPTION`/`VALUE` 动作的实现和 `SET_SPAN` 的格式问题。
