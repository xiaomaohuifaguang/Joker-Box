# 动态表单字段联动规则设计文档

> 文档版本：2026-05-21
> 适用范围：动态表单字段联动规则配置与前后端联调

---

## 目录

1. [概述](#1-概述)
2. [数据模型](#2-数据模型)
3. [条件树结构](#3-条件树结构)
4. [条件运算符](#4-条件运算符)
5. [动作类型](#5-动作类型)
6. [字段类型与动作兼容性](#6-字段类型与动作兼容性)
7. [执行机制](#7-执行机制)
8. [前后端职责边界](#8-前后端职责边界)
9. [配置示例](#9-配置示例)
10. [前端实现指南](#10-前端实现指南)

---

## 1. 概述

### 1.1 什么是字段联动

字段联动是指：当表单中某个（或多个）字段的值满足特定条件时，自动改变另一个字段的状态或属性。例如：

- 当"是否已婚"选择"是"时，"配偶姓名"字段变为必填并显示
- 当"证件类型"选择"身份证"时，"证件号码"的正则校验自动切换为身份证格式
- 当"所在省份"选择"浙江"时，"所在城市"的下拉选项自动切换为浙江省的城市列表

### 1.2 核心概念

| 概念 | 说明 |
|------|------|
| **触发字段** | 被监控的字段，其值变化会触发联动判断 |
| **触发条件** | 对触发字段值的判断条件（如等于、大于、包含等） |
| **触发值** | 用于比较的目标值 |
| **目标字段** | 联动动作作用的字段 |
| **动作类型** | 满足条件后对目标字段执行的操作 |
| **动作参数** | 动作需要的额外参数 |
| **条件树** | 支持嵌套 AND / OR 的条件组合结构 |

### 1.3 支持的联动能力总览

| 能力 | 说明 | 后端支持 | 前端需配合 |
|------|------|---------|-----------|
| 显示/隐藏 | SHOW / HIDE | 是 | 是 |
| 必填/非必填 | REQUIRED | 是 | 是 |
| 禁用/启用 | DISABLED / ENABLED | 是 | 是 |
| 设置正则 | SET_PATTERN | 是 | 是 |
| 设置宽度 | SET_SPAN | 是 | 是 |
| 设置选项 | OPTION | 否 | 必须 |
| 设置值 | VALUE | 否 | 必须 |

---

## 2. 数据模型

### 2.1 联动规则表 `cat_dynamic_form_linkage_rule`

```sql
CREATE TABLE `cat_dynamic_form_linkage_rule` (
  `id` varchar(64) NOT NULL COMMENT '主键ID',
  `form_id` varchar(64) NOT NULL COMMENT '表单ID',
  `version` varchar(32) NOT NULL COMMENT '表单版本号',
  `name` varchar(128) COMMENT '规则名称（供前端展示）',
  `target_field_id` varchar(64) NOT NULL COMMENT '目标字段 fieldId（前端设计ID）',
  `action_type` varchar(32) NOT NULL COMMENT '动作类型',
  `action_value` json COMMENT '动作参数，JSON格式',
  `enable` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '规则执行顺序，越小越先执行',
  `deleted` varchar(1) NOT NULL DEFAULT '0',
  `create_by` varchar(64) COMMENT '创建人',
  `create_time` datetime COMMENT '创建时间',
  `update_time` datetime COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_form_version`(`form_id`, `version`),
  INDEX `idx_target`(`target_field_id`)
);
```

### 2.2 联动条件节点表 `cat_dynamic_form_linkage_node`

```sql
CREATE TABLE `cat_dynamic_form_linkage_node` (
  `id` varchar(64) NOT NULL COMMENT '主键ID',
  `rule_id` varchar(64) NOT NULL COMMENT '所属规则ID',
  `form_id` varchar(64) NOT NULL DEFAULT '' COMMENT '表单ID',
  `version` varchar(32) NOT NULL DEFAULT '' COMMENT '版本号',
  `parent_id` varchar(64) COMMENT '父节点ID，null表示根节点',
  `node_type` varchar(16) NOT NULL COMMENT '节点类型：AND/OR/CONDITION',
  `trigger_field_id` varchar(64) COMMENT '触发字段 fieldId，仅CONDITION节点有效',
  `trigger_condition` varchar(32) COMMENT '触发条件，仅CONDITION节点有效',
  `trigger_value` json COMMENT '触发值，仅CONDITION节点有效',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '同级节点排序',
  `deleted` varchar(1) NOT NULL DEFAULT '0',
  `create_by` varchar(64) COMMENT '创建人',
  `create_time` datetime COMMENT '创建时间',
  `update_time` datetime COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_rule_id`(`rule_id`),
  INDEX `idx_parent_id`(`parent_id`),
  INDEX `idx_node_type`(`node_type`),
  INDEX `idx_trigger_field`(`trigger_field_id`),
  INDEX `idx_form_version`(`form_id`, `version`)
);
```

### 2.3 实体类结构

#### DynamicFormLinkageRule（联动规则）

```java
public class DynamicFormLinkageRule {
    private String id;              // 规则ID（UUID）
    private String formId;          // 表单ID
    private String version;         // 版本号
    private String name;            // 规则名称（前端展示用）
    private String targetFieldId;   // 目标字段 fieldId
    private String actionType;      // 动作类型
    private Object actionValue;     // 动作参数（JSON，由 JsonValueTypeHandler 处理）
    private Boolean enable;         // 是否启用（true/false）
    private Integer sortOrder;      // 执行顺序（越小越先）
    private List<DynamicFormLinkageNode> conditionTree;  // 条件节点树（不持久化，运行时组装）
}
```

#### DynamicFormLinkageNode（条件节点）

```java
public class DynamicFormLinkageNode {
    private String id;              // 节点ID（UUID）
    private String ruleId;          // 所属规则ID
    private String formId;          // 表单ID
    private String version;         // 版本号
    private String parentId;        // 父节点ID（null = 根节点）
    private String nodeType;        // 节点类型：AND / OR / CONDITION
    private String triggerFieldId;  // 触发字段 fieldId（仅 CONDITION）
    private String triggerCondition;// 触发条件（仅 CONDITION）
    private Object triggerValue;    // 触发值（JSON，仅 CONDITION）
    private Integer sortOrder;      // 同级排序
    private List<DynamicFormLinkageNode> children;  // 子节点（不持久化，运行时组装）
}
```

### 2.4 数据持久化说明

- `conditionTree` 和 `children` 字段标注 `@TableField(exist = false)`，**不直接持久化**
- 条件树通过 `parentId` 关联拆分为平表存储，查询后由业务层组装为树形结构
- `triggerValue` 和 `actionValue` 使用 `JsonValueTypeHandler` 处理，支持任意 JSON 类型

---

## 3. 条件树结构

### 3.1 树形结构说明

条件树是一颗由逻辑节点（AND/OR）和叶子节点（CONDITION）组成的树：

- **AND 节点**：所有子节点都为真时结果为真（短路：第一个假立即返回假）
- **OR 节点**：任一子节点为真时结果为真（短路：第一个真立即返回真）
- **CONDITION 节点**：对单个触发字段进行条件判断，无子节点

### 3.2 树的根节点约定

每棵条件树的第一个元素为根节点，类型必须是 AND 或 OR。例如：

```json
{
  "conditionTree": [
    {
      "nodeType": "AND",
      "children": [
        { "nodeType": "CONDITION", "triggerFieldId": "gender", "triggerCondition": "EQ", "triggerValue": "female" },
        {
          "nodeType": "OR",
          "children": [
            { "nodeType": "CONDITION", "triggerFieldId": "age", "triggerCondition": "GE", "triggerValue": 18 },
            { "nodeType": "CONDITION", "triggerFieldId": "hasGuardian", "triggerCondition": "EQ", "triggerValue": true }
          ]
        }
      ]
    }
  ]
}
```

**逻辑含义**：性别为女性 **且**（年龄大于等于18 **或** 有监护人）

### 3.3 简单条件（单条件）

单个条件时，根节点 AND 下只有一个 CONDITION 子节点：

```json
{
  "conditionTree": [
    {
      "nodeType": "AND",
      "children": [
        { "nodeType": "CONDITION", "triggerFieldId": "isMarried", "triggerCondition": "EQ", "triggerValue": "true" }
      ]
    }
  ]
}
```

### 3.4 节点判空处理

| 场景 | 行为 |
|------|------|
| 节点为 null | 返回 true（视为满足） |
| 逻辑节点无 children | 返回 true（视为满足） |
| 所有子节点遍历完毕 | AND 返回 true，OR 返回 false |

---

## 4. 条件运算符

### 4.1 运算符总览

| 运算符 | 含义 | triggerValue 类型 | 适用字段类型 |
|--------|------|------------------|-------------|
| `EQ` | 等于 | String / Number / Boolean / null | 全部 |
| `NE` | 不等于 | String / Number / Boolean / null | 全部 |
| `GT` | 大于 | Number | NUMBER, RATE, SLIDER |
| `LT` | 小于 | Number | NUMBER, RATE, SLIDER |
| `GE` | 大于等于 | Number | NUMBER, RATE, SLIDER |
| `LE` | 小于等于 | Number | NUMBER, RATE, SLIDER |
| `IN` | 包含于 | Array / Collection | 全部（多值字段效果最佳） |
| `NOT_IN` | 不包含于 | Array / Collection | 全部（多值字段效果最佳） |
| `EMPTY` | 为空 | 无需填写（null 即可） | 全部 |
| `NOT_EMPTY` | 非空 | 无需填写（null 即可） | 全部 |
| `REGEX` | 正则匹配 | String（正则表达式） | INPUT, TEXTAREA |

### 4.2 运算符详细说明

#### EQ（等于）

```java
Objects.equals(actual, expect)
```

- 使用 `Objects.equals` 比较，null 安全
- 对于字符串，区分大小写
- 对于数值类型，比较的是原始值，不是字符串形式

**示例**：

```json
{ "triggerCondition": "EQ", "triggerValue": "married" }
// 当触发字段值等于 "married" 时满足
```

#### NE（不等于）

```java
!Objects.equals(actual, expect)
```

- EQ 的逻辑取反

#### GT / LT / GE / LE（数值比较）

```java
double a = Double.parseDouble(actual.toString());
double e = Double.parseDouble(expect.toString());
```

- 将实际值和触发值都转为 `double` 后比较
- **任一值为 null** → 返回 false
- **任一值无法解析为数字**（NumberFormatException）→ 返回 false
- 支持整数和小数比较

**示例**：

```json
{ "triggerCondition": "GE", "triggerValue": 18 }
// 当触发字段值 >= 18 时满足

{ "triggerCondition": "GT", "triggerValue": 0 }
// 当触发字段值 > 0 时满足
```

**注意**：对于 RATE / SLIDER 等字段，前端传 number 类型，后端会 toString 后 parseDouble。

#### IN（包含于）

```java
if (expect instanceof Collection)  collection.contains(actual);
if (expect instanceof Object[])    Arrays.asList(arr).contains(actual);
else                              Objects.equals(actual, expect);
```

- `triggerValue` 为数组时：判断 `actual` 是否在数组中
- `triggerValue` 为单个值时：退化为 EQ 比较
- 比较使用 `Objects.equals`，null 安全

**示例**：

```json
{ "triggerCondition": "IN", "triggerValue": ["zhejiang", "jiangsu", "shanghai"] }
// 当触发字段值在列表中时满足
```

**适用场景**：
- SELECT / RADIO：判断当前选中值是否在某个范围内
- MULTISELECT / CHECKBOX：判断用户选中的**某一个值**是否在范围内（注意：不会自动展开数组的每个元素逐一判断）

#### NOT_IN（不包含于）

IN 的逻辑取反。

#### EMPTY（为空）

```java
!StringUtils.hasText(strActual)
```

- 实际值为 null → 视为空
- 实际值为空字符串 `""` → 视为空
- 实际值为仅包含空白字符的字符串 `"   "` → 视为空
- **注意**：对于数组类型字段（如 MULTISELECT），空数组 `[]` 的 toString 是 `"[]"`，**不会被视为空**

**示例**：

```json
{ "triggerCondition": "EMPTY" }
// triggerValue 无需填写（或填 null）
```

#### NOT_EMPTY（非空）

EMPTY 的逻辑取反。

#### REGEX（正则匹配）

```java
Pattern.matches(expect.toString(), strActual)
```

- `triggerValue` 为正则表达式字符串
- 实际值为 null 或空字符串 → 返回 false
- 正则语法错误（PatternSyntaxException）→ 返回 false
- 使用 `Pattern.matches`，即**完全匹配**（不是部分匹配，相当于自动加了 `^...$`）

**示例**：

```json
{ "triggerCondition": "REGEX", "triggerValue": "^1[3-9]\\d{9}$" }
// 当触发字段值匹配手机号格式时满足
```

### 4.3 特殊字段类型的条件判断说明

| 字段类型 | 实际值 (actual) 格式 | 条件判断注意事项 |
|----------|---------------------|-----------------|
| INPUT / TEXTAREA | String | 直接使用 |
| NUMBER / RATE / SLIDER | number（前端）→ 后端 toString | GT/LT/GE/LE 比较数值 |
| SELECT / RADIO | String（选中的 value） | EQ/NE 比较 |
| MULTISELECT / CHECKBOX | String[]（选中的 value 数组） | EQ 比较整个数组对象；IN 比较数组引用 |
| SWITCH | boolean（前端）→ 后端 "true"/"false" | 建议用 EQ + "true" / "false" |
| DATE / DATETIME / TIME | String | 按字符串比较（非日期语义） |
| DATERANGE | String[] `[start, end]` | EQ 比较整个数组对象 |
| CASCADER | String（选中节点的 value） | EQ/NE 比较 |
| MULTICASCADER | String[] | EQ 比较整个数组对象 |
| UPLOAD | FileInfo[] | 不建议作为触发字段 |
| COLOR | String（如 `#FF0000`） | 按字符串比较 |

**重要提示**：
- MULTISELECT / CHECKBOX / MULTICASCADER / DATERANGE 等数组类型字段，当作为**触发字段**时，`actual` 是整个数组对象。`EQ` 比较的是数组引用，不是数组内容。建议此类字段使用 `EMPTY` / `NOT_EMPTY` 判断，或避免作为数值比较型条件字段。

---

## 5. 动作类型

### 5.1 动作类型总览

| 动作类型 | 作用 | actionValue 类型 | 后端支持 | 前端必须 |
|----------|------|-----------------|---------|---------|
| `SHOW` | 显示目标字段 | 无（null 即可） | 是 | 是 |
| `HIDE` | 隐藏目标字段 | 无（null 即可） | 是 | 是 |
| `REQUIRED` | 设置必填状态 | Boolean（默认 true） | 是 | 是 |
| `DISABLED` | 设置禁用状态 | Boolean（默认 true） | 是 | 是 |
| `ENABLED` | 启用目标字段 | 无（null 即可） | 是 | 是 |
| `SET_PATTERN` | 设置正则校验 | Object { pattern, patternTips } | 是 | 是 |
| `SET_SPAN` | 设置字段宽度 | Object { span } | 是 | 是 |
| `OPTION` | 设置选项列表 | Array（选项数组） | **否** | **必须** |
| `VALUE` | 设置字段值 | 任意（字段类型对应格式） | **否** | **必须** |

### 5.2 后端支持的动作（LinkageValidator 处理）

后端 `LinkageValidator.applyAction()` 对以下动作有处理逻辑：

#### SHOW

```java
effect.visible = true;
```

- 使目标字段可见
- actionValue 被忽略

#### HIDE

```java
effect.visible = false;
```

- 使目标字段隐藏
- actionValue 被忽略

#### REQUIRED

```java
boolean required = actionValue == null || !Boolean.FALSE.equals(actionValue);
effect.required = required;
```

- actionValue 为 null → 设为必填（true）
- actionValue 为 true → 设为必填（true）
- actionValue 为 false → 设为非必填（false）

**示例**：

```json
{ "actionType": "REQUIRED", "actionValue": true }
// 目标字段变为必填

{ "actionType": "REQUIRED", "actionValue": false }
// 目标字段变为非必填

{ "actionType": "REQUIRED", "actionValue": null }
// 目标字段变为必填（默认行为）
```

#### DISABLED

```java
boolean disabled = actionValue == null || !Boolean.FALSE.equals(actionValue);
effect.disabled = disabled;
```

- 逻辑同 REQUIRED
- actionValue 为 null/true → 禁用
- actionValue 为 false → 启用

#### ENABLED

```java
effect.disabled = false;
```

- 使目标字段可用（解除禁用）
- actionValue 被忽略
- 与 `DISABLED` + `false` 效果等价，但更简洁

#### SET_PATTERN

```java
if (actionValue instanceof Map<?, ?> m) {
    effect.pattern = (String) m.get("pattern");
    effect.patternTips = (String) m.get("patternTips");
}
```

- actionValue 必须是对象格式，包含 `pattern` 和 `patternTips`
- 仅对 INPUT、TEXTAREA 等支持正则校验的字段有效
- 设置后**覆盖**字段原有的 pattern / patternTips

**示例**：

```json
{
  "actionType": "SET_PATTERN",
  "actionValue": {
    "pattern": "^\\d{17}[\\dXx]$",
    "patternTips": "请输入正确的18位身份证号"
  }
}
```

#### SET_SPAN

```java
if (actionValue instanceof Map<?, ?> m) {
    Object v = m.get("span");
    if (v instanceof Number n) {
        effect.span = n.intValue();
    }
}
```

- actionValue 必须是对象格式，包含 `span`
- span 值为整数，范围 1-24
- 设置后**覆盖**字段原有的 span

**示例**：

```json
{
  "actionType": "SET_SPAN",
  "actionValue": {
    "span": 12
  }
}
// 目标字段宽度变为 12（占半行）
```

### 5.3 前端独占的动作

以下动作**后端 LinkageValidator 不处理**，完全由前端实现：

#### OPTION（设置选项）

**功能**：动态修改目标字段的下拉选项列表。

**actionValue 格式**：

```json
[
  { "value": "zhejiang", "label": "浙江" },
  { "value": "jiangsu", "label": "江苏" }
]
```

**适用字段类型**：
- SELECT（单选下拉）
- MULTISELECT（多选下拉）
- RADIO（单选按钮）
- CHECKBOX（多选框）
- CASCADER / MULTICASCADER（级联选择，需提供树形选项）

**前端实现要点**：
- 监听触发字段变化，条件满足时将目标字段的 options 替换为 actionValue
- 条件不满足时，恢复为字段原始 options
- 如果目标字段已选值不在新选项中，建议清空或保留（视业务需求）

#### VALUE（设置值）

**功能**：条件满足时自动填充目标字段的值。

**actionValue 格式**：字段类型对应的值格式（见下表）

| 目标字段类型 | actionValue 示例 |
|-------------|-----------------|
| INPUT / TEXTAREA | `"自动填充的文本"` |
| NUMBER / RATE / SLIDER | `100` 或 `"100"` |
| SELECT / RADIO | `"optionValue"` |
| MULTISELECT / CHECKBOX | `["opt1", "opt2"]` |
| SWITCH | `true` 或 `"true"` |
| DATE / DATETIME / TIME | `"2026-05-21"` |
| DATERANGE | `["2026-05-01", "2026-05-21"]` |
| CASCADER | `"xihu"` |
| MULTICASCADER | `["xihu", "gulou"]` |
| COLOR | `"#FF0000"` |

**前端实现要点**：
- 条件满足时，将目标字段值设为 actionValue
- 条件从满足变为不满足时，**建议恢复为 defaultValue 或清空**（需前端决策）
- 被联动设置的值，用户仍可手动修改（除非字段同时被 DISABLED）
- VALUE 联动可能与用户输入冲突，建议配合 DISABLED 使用

### 5.4 默认值与恢复机制

**后端返回的效果默认值**：

| 属性 | 默认值 | 含义 |
|------|--------|------|
| visible | true | 默认显示 |
| required | false | 默认非必填 |
| disabled | false | 默认可用 |
| pattern | null | 使用字段原始正则 |
| patternTips | null | 使用字段原始提示 |
| span | null | 使用字段原始宽度 |

**恢复逻辑**：
- 后端只返回当前条件下计算出的效果值
- 当条件从满足变为不满足时，前端需要将字段状态恢复为默认值
- 前端应保存字段的**原始配置**（原始 pattern、span、options），以便恢复

---

## 6. 字段类型与动作兼容性

### 6.1 兼容性矩阵

| 字段类型 | SHOW/HIDE | REQUIRED | DISABLED/ENABLED | SET_PATTERN | SET_SPAN | OPTION | VALUE |
|----------|:---------:|:--------:|:----------------:|:-----------:|:--------:|:------:|:-----:|
| INPUT | Y | Y | Y | Y | Y | - | Y |
| NUMBER | Y | Y | Y | - | Y | - | Y |
| SELECT | Y | Y | Y | - | Y | Y | Y |
| MULTISELECT | Y | Y | Y | - | Y | Y | Y |
| RADIO | Y | Y | Y | - | Y | Y | Y |
| CHECKBOX | Y | Y | Y | - | Y | Y | Y |
| DATE | Y | Y | Y | - | Y | - | Y |
| DATETIME | Y | Y | Y | - | Y | - | Y |
| TIME | Y | Y | Y | - | Y | - | Y |
| DATERANGE | Y | Y | Y | - | Y | - | Y |
| SWITCH | Y | Y | Y | - | Y | - | Y |
| TEXTAREA | Y | Y | Y | Y | Y | - | Y |
| UPLOAD | Y | Y | Y | - | Y | - | Y |
| RATE | Y | Y | Y | - | Y | - | Y |
| SLIDER | Y | Y | Y | - | Y | - | Y |
| COLOR | Y | Y | Y | - | Y | - | Y |
| CASCADER | Y | Y | Y | - | Y | Y | Y |
| MULTICASCADER | Y | Y | Y | - | Y | Y | Y |

**图例**：
- Y = 支持且有意义
- - = 不支持或无意义（前端可忽略或报错提示）

### 6.2 触发字段适用性

| 字段类型 | 作为触发字段的适用性 | 推荐使用的条件运算符 |
|----------|---------------------|---------------------|
| INPUT | 一般（文本匹配） | EQ, NE, EMPTY, NOT_EMPTY, REGEX |
| NUMBER | 优秀（数值比较） | EQ, NE, GT, LT, GE, LE, EMPTY, NOT_EMPTY |
| SELECT / RADIO | 优秀（离散值匹配） | EQ, NE, IN, NOT_IN, EMPTY, NOT_EMPTY |
| MULTISELECT / CHECKBOX | 一般（数组比较） | EMPTY, NOT_EMPTY（不推荐数值比较） |
| SWITCH | 优秀（布尔判断） | EQ, NE（triggerValue 用 `"true"` / `"false"`） |
| DATE / DATETIME / TIME | 一般（字符串比较） | EQ, NE, EMPTY, NOT_EMPTY（非日期语义比较） |
| DATERANGE | 差（数组对象） | EMPTY, NOT_EMPTY |
| CASCADER | 一般 | EQ, NE, IN, NOT_IN, EMPTY, NOT_EMPTY |
| MULTICASCADER | 差（数组对象） | EMPTY, NOT_EMPTY |
| UPLOAD | 差 | EMPTY, NOT_EMPTY |
| RATE / SLIDER | 优秀 | EQ, NE, GT, LT, GE, LE |
| COLOR | 一般 | EQ, NE, EMPTY, NOT_EMPTY |
| TEXTAREA | 一般 | EQ, NE, EMPTY, NOT_EMPTY, REGEX |

---

## 7. 执行机制

### 7.1 规则执行流程

```
输入：规则列表（已按 sortOrder 排序），表单数据 data
输出：每个字段的效果 Map<fieldId, FieldEffect>

1. 初始化空的效果映射表
2. 遍历每条规则（按 sortOrder 升序）：
   a. 如果规则被禁用（enable=false），跳过
   b. 如果规则无条件树，跳过
   c. 取条件树根节点（conditionTree[0]）
   d. 递归求值节点树：
      - CONDITION：match(触发字段值, 条件运算符, 触发值)
      - AND：所有子节点为真则真（短路）
      - OR：任一子节点为真则真（短路）
   e. 如果条件满足：
      - 获取/创建目标字段的 MutableEffect
      - 应用动作（修改 effect 的属性）
3. 将 MutableEffect 转换为不可变的 FieldEffect
4. 返回效果映射表
```

### 7.2 执行顺序与覆盖规则

**规则排序**：
- 规则按 `sortOrder` 升序执行（sortOrder 越小越先执行）
- 同 sortOrder 的规则，按查询返回顺序执行（不建议依赖）

**覆盖规则**：
- 多条规则作用于**同一目标字段**时，**后执行的规则覆盖先执行的规则**
- 例如：规则A（sortOrder=1）HIDE 字段X；规则B（sortOrder=2）SHOW 字段X → 最终字段X可见

**建议**：
- 为同一目标字段配置规则时，注意 sortOrder 的顺序是否符合业务预期
- 复杂的联动逻辑建议通过条件树组合，而不是拆分为多条规则

### 7.3 字段效果计算示例

假设有三条规则作用于字段 `spouseName`：

| sortOrder | 条件 | 动作 |
|-----------|------|------|
| 1 | isMarried EQ "true" | SHOW |
| 2 | isMarried EQ "true" | REQUIRED |
| 3 | age LT 18 | HIDE |

**场景1**：isMarried="true", age=25
- 规则1 满足 → spouseName.visible = true
- 规则2 满足 → spouseName.required = true
- 规则3 不满足 → 无影响
- **结果**：visible=true, required=true, disabled=false

**场景2**：isMarried="true", age=16
- 规则1 满足 → spouseName.visible = true
- 规则2 满足 → spouseName.required = true
- 规则3 满足 → spouseName.visible = false（覆盖规则1的 SHOW）
- **结果**：visible=false, required=true, disabled=false
- **注意**：虽然字段被隐藏，但 required 仍为 true。前端应在字段隐藏时自动将其视为非必填，或后端配合处理。

### 7.4 隐藏字段的校验处理建议

**问题**：被 HIDE 的目标字段，如果 REQUIRED=true，提交时会因校验失败而报错。

**解决方案**（前端实现）：
1. 字段被隐藏（visible=false）时，前端不应将其值包含在提交数据中
2. 或者，后端在校验时自动忽略 hidden 字段的 required 检查

**当前实现**：后端 `LinkageValidator` 只计算效果，不修改校验逻辑。建议前端在提交时过滤掉 hidden 字段。

---

## 8. 前后端职责边界

### 8.1 后端职责

| 职责 | 说明 |
|------|------|
| 规则持久化 | 保存/查询联动规则和条件节点 |
| 条件求值 | 根据表单数据计算条件树是否满足 |
| 效果计算 | 计算每个目标字段的 visible / required / disabled / pattern / patternTips / span |
| 接口暴露 | 提供接口返回字段效果（或前端自行计算） |

**后端返回数据结构示例**：

```json
{
  "spouseName": {
    "visible": true,
    "required": true,
    "disabled": false
  },
  "spousePhone": {
    "visible": true,
    "required": true,
    "disabled": false,
    "pattern": "^1[3-9]\\d{9}$",
    "patternTips": "请输入正确的手机号"
  }
}
```

### 8.2 前端职责

| 职责 | 说明 |
|------|------|
| 规则配置 UI | 提供可视化界面配置联动规则 |
| 条件树组装 | 将 UI 配置转换为条件树结构提交给后端 |
| 实时联动计算 | 字段值变化时，调用后端接口（或前端本地计算）获取最新效果 |
| 字段状态渲染 | 根据效果值控制字段的显隐、必填、禁用状态 |
| 正则动态绑定 | 根据 pattern 动态修改校验规则 |
| 宽度动态调整 | 根据 span 动态调整字段布局 |
| **OPTION 动作** | 动态修改字段的 options（后端不处理，必须前端实现） |
| **VALUE 动作** | 自动填充字段值（后端不处理，必须前端实现） |
| 提交数据过滤 | 隐藏字段（visible=false）的值不应提交 |
| 恢复机制 | 条件不满足时恢复字段原始状态 |

### 8.3 联动计算方式选择

**方案A：前端本地计算（推荐）**

- 前端加载表单时，同时获取所有联动规则
- 字段值变化时，前端本地执行 LinkageValidator 的等价逻辑
- 优点：响应快，无额外请求
- 缺点：前后端逻辑需保持一致

**方案B：后端实时计算**

- 字段值变化时，前端发送当前表单数据到后端
- 后端计算并返回各字段效果
- 优点：逻辑集中，避免前后端不一致
- 缺点：频繁请求，体验较差

**建议**：
- 采用方案A，前端实现与后端 `LinkageValidator` 完全一致的求值逻辑
- 后端提供规则查询接口，前端获取规则后本地计算
- 提交时后端再次校验（防止绕过前端）

---

## 9. 配置示例

### 9.1 示例1：简单的显示/隐藏联动

**需求**：当"是否已婚"选择"是"时，显示"配偶姓名"字段。

**规则配置**：

```json
{
  "name": "已婚显示配偶姓名",
  "targetFieldId": "spouseName",
  "actionType": "SHOW",
  "actionValue": null,
  "enable": true,
  "sortOrder": 1,
  "conditionTree": [
    {
      "nodeType": "AND",
      "children": [
        {
          "nodeType": "CONDITION",
          "triggerFieldId": "isMarried",
          "triggerCondition": "EQ",
          "triggerValue": "true"
        }
      ]
    }
  ]
}
```

**配套规则**（配偶姓名必填）：

```json
{
  "name": "已婚配偶姓名必填",
  "targetFieldId": "spouseName",
  "actionType": "REQUIRED",
  "actionValue": true,
  "enable": true,
  "sortOrder": 2,
  "conditionTree": [
    {
      "nodeType": "AND",
      "children": [
        {
          "nodeType": "CONDITION",
          "triggerFieldId": "isMarried",
          "triggerCondition": "EQ",
          "triggerValue": "true"
        }
      ]
    }
  ]
}
```

### 9.2 示例2：多条件组合（AND + OR）

**需求**：当"用户类型"为"VIP" **且**（"消费金额" >= 10000 **或** "注册年限" >= 3）时，显示"专属客服"字段。

```json
{
  "name": "VIP大客户显示专属客服",
  "targetFieldId": "exclusiveService",
  "actionType": "SHOW",
  "actionValue": null,
  "enable": true,
  "sortOrder": 1,
  "conditionTree": [
    {
      "nodeType": "AND",
      "children": [
        {
          "nodeType": "CONDITION",
          "triggerFieldId": "userType",
          "triggerCondition": "EQ",
          "triggerValue": "VIP"
        },
        {
          "nodeType": "OR",
          "children": [
            {
              "nodeType": "CONDITION",
              "triggerFieldId": "consumeAmount",
              "triggerCondition": "GE",
              "triggerValue": 10000
            },
            {
              "nodeType": "CONDITION",
              "triggerFieldId": "registerYears",
              "triggerCondition": "GE",
              "triggerValue": 3
            }
          ]
        }
      ]
    }
  ]
}
```

### 9.3 示例3：动态正则校验

**需求**：当"证件类型"选择不同值时，"证件号码"的正则校验规则随之变化。

**规则1：身份证格式**

```json
{
  "name": "身份证格式校验",
  "targetFieldId": "idNumber",
  "actionType": "SET_PATTERN",
  "actionValue": {
    "pattern": "^\\d{17}[\\dXx]$",
    "patternTips": "请输入正确的18位身份证号"
  },
  "enable": true,
  "sortOrder": 1,
  "conditionTree": [
    {
      "nodeType": "AND",
      "children": [
        {
          "nodeType": "CONDITION",
          "triggerFieldId": "idType",
          "triggerCondition": "EQ",
          "triggerValue": "ID_CARD"
        }
      ]
    }
  ]
}
```

**规则2：护照格式**

```json
{
  "name": "护照格式校验",
  "targetFieldId": "idNumber",
  "actionType": "SET_PATTERN",
  "actionValue": {
    "pattern": "^[a-zA-Z0-9]{7,15}$",
    "patternTips": "请输入正确的护照号码"
  },
  "enable": true,
  "sortOrder": 2,
  "conditionTree": [
    {
      "nodeType": "AND",
      "children": [
        {
          "nodeType": "CONDITION",
          "triggerFieldId": "idType",
          "triggerCondition": "EQ",
          "triggerValue": "PASSPORT"
        }
      ]
    }
  ]
}
```

### 9.4 示例4：动态选项（前端实现）

**需求**：当"所在省份"变化时，"所在城市"的选项列表随之变化。

```json
{
  "name": "浙江省城市选项",
  "targetFieldId": "city",
  "actionType": "OPTION",
  "actionValue": [
    { "value": "hangzhou", "label": "杭州" },
    { "value": "ningbo", "label": "宁波" },
    { "value": "wenzhou", "label": "温州" }
  ],
  "enable": true,
  "sortOrder": 1,
  "conditionTree": [
    {
      "nodeType": "AND",
      "children": [
        {
          "nodeType": "CONDITION",
          "triggerFieldId": "province",
          "triggerCondition": "EQ",
          "triggerValue": "zhejiang"
        }
      ]
    }
  ]
}
```

**注意**：此规则的 OPTION 动作由前端处理，后端只保存规则，不参与选项切换逻辑。

### 9.5 示例5：自动填充值（前端实现）

**需求**：当"是否同意协议"选择"是"时，自动将"签署日期"填充为今天。

```json
{
  "name": "同意协议自动填充日期",
  "targetFieldId": "signDate",
  "actionType": "VALUE",
  "actionValue": "2026-05-21",
  "enable": true,
  "sortOrder": 1,
  "conditionTree": [
    {
      "nodeType": "AND",
      "children": [
        {
          "nodeType": "CONDITION",
          "triggerFieldId": "agreeProtocol",
          "triggerCondition": "EQ",
          "triggerValue": "true"
        }
      ]
    }
  ]
}
```

**注意**：此规则的 VALUE 动作由前端处理，后端只保存规则配置。

### 9.6 示例6：禁用联动

**需求**：当"订单状态"为"已完成"时，所有编辑字段禁用。

```json
{
  "name": "已完成订单禁用编辑",
  "targetFieldId": "orderAmount",
  "actionType": "DISABLED",
  "actionValue": true,
  "enable": true,
  "sortOrder": 1,
  "conditionTree": [
    {
      "nodeType": "AND",
      "children": [
        {
          "nodeType": "CONDITION",
          "triggerFieldId": "orderStatus",
          "triggerCondition": "EQ",
          "triggerValue": "completed"
        }
      ]
    }
  ]
}
```

**注意**：需要为每个目标字段分别配置规则，或使用相同条件配置多条规则。

---

## 10. 前端实现指南

### 10.1 联动规则配置界面设计建议

#### 规则列表

```
┌─────────────────────────────────────────┐
│ 联动规则                                    │
├─────────────────────────────────────────┤
│ ① 已婚显示配偶姓名 [编辑] [删除] [启用/禁用]  │
│ ② 已婚配偶姓名必填 [编辑] [删除] [启用/禁用]  │
│ ③ VIP大客户显示专属客服 [编辑] [删除]...      │
│                                         │
│ [+ 新增规则]                              │
└─────────────────────────────────────────┘
```

#### 规则编辑表单

```
规则名称: [________________]
目标字段: [下拉选择字段 ▼]
动作类型: [下拉选择动作 ▼]
动作参数: [根据动作类型动态显示输入项]

执行顺序: [____] (数字越小越先执行)
是否启用: [开关]

条件配置:
┌─────────────────────────────────────────┐
│ [全部满足] / [任一满足]                   │
│                                         │
│ ① 当 [字段A ▼] [等于 ▼] [_______]        │
│ ② 当 [字段B ▼] [大于 ▼] [_______]        │
│ ③ [且] / [或]                            │
│    ③-1 当 [字段C ▼] [包含 ▼] [____]      │
│    ③-2 当 [字段D ▼] [为空 ▼]             │
│                                         │
│ [+ 添加条件] [+ 添加条件组]                │
└─────────────────────────────────────────┘
```

### 10.2 条件树与 UI 的映射

**UI 的"条件组"对应 AND/OR 节点**：

```
UI 结构                          条件树结构
-------                          ---------
[全部满足]                       AND (根节点)
  ├─ 条件1                        ├─ CONDITION
  ├─ 条件2                        ├─ CONDITION
  └─ [任一满足]                   └─ OR
       ├─ 条件3                       ├─ CONDITION
       └─ 条件4                       └─ CONDITION
```

### 10.3 字段值变化监听

```typescript
// Vue3 伪代码
watch(formData, (newVal, oldVal) => {
  // 获取所有联动规则
  const rules = loadedLinkageRules;

  // 计算字段效果
  const effects = evalFieldEffects(rules, newVal);

  // 应用效果到字段
  for (const [fieldId, effect] of Object.entries(effects)) {
    const field = findFieldById(fieldId);
    if (!field) continue;

    // 应用 visible
    field._visible = effect.visible;

    // 应用 required
    field._required = effect.required;

    // 应用 disabled
    field._disabled = effect.disabled;

    // 应用 pattern（如有）
    if (effect.pattern !== undefined && effect.pattern !== null) {
      field._pattern = effect.pattern;
      field._patternTips = effect.patternTips;
    }

    // 应用 span（如有）
    if (effect.span !== undefined && effect.span !== null) {
      field._span = effect.span;
    }
  }

  // 处理前端独占动作（OPTION, VALUE）
  for (const rule of rules) {
    if (!rule.enable) continue;
    const root = rule.conditionTree?.[0];
    if (evaluateNode(root, newVal)) {
      if (rule.actionType === 'OPTION') {
        const targetField = findFieldById(rule.targetFieldId);
        if (targetField) {
          targetField.options = rule.actionValue;
        }
      }
      if (rule.actionType === 'VALUE') {
        formData[rule.targetFieldId] = rule.actionValue;
      }
    }
  }
}, { deep: true });
```

### 10.4 提交数据过滤

```typescript
function buildSubmitData(formData: Record<string, any>, fields: FieldConfig[]) {
  const result: Record<string, any> = {};

  for (const [fieldId, value] of Object.entries(formData)) {
    const field = findFieldById(fieldId);
    if (!field) continue;

    // 过滤隐藏字段
    if (field._visible === false) continue;

    result[fieldId] = value;
  }

  return result;
}
```

### 10.5 条件求值实现（前端）

```typescript
function evaluateNode(node: LinkageNode, data: Record<string, any>): boolean {
  if (!node) return true;

  if (node.nodeType === 'CONDITION') {
    const actual = data[node.triggerFieldId];
    return match(actual, node.triggerCondition, node.triggerValue);
  }

  const children = node.children || [];
  if (children.length === 0) return true;

  const isAnd = node.nodeType === 'AND';
  for (const child of children) {
    const childResult = evaluateNode(child, data);
    if (isAnd && !childResult) return false;
    if (!isAnd && childResult) return true;
  }
  return isAnd;
}

function match(actual: any, condition: string, expect: any): boolean {
  const strActual = actual == null ? '' : String(actual);

  switch (condition) {
    case 'EQ': return actual === expect;
    case 'NE': return actual !== expect;
    case 'EMPTY': return !strActual || strActual.trim() === '';
    case 'NOT_EMPTY': return !!strActual && strActual.trim() !== '';
    case 'GT':
    case 'LT':
    case 'GE':
    case 'LE':
      const a = parseFloat(strActual);
      const e = parseFloat(String(expect));
      if (isNaN(a) || isNaN(e)) return false;
      switch (condition) {
        case 'GT': return a > e;
        case 'LT': return a < e;
        case 'GE': return a >= e;
        case 'LE': return a <= e;
      }
      break;
    case 'IN':
      if (Array.isArray(expect)) return expect.includes(actual);
      return actual === expect;
    case 'NOT_IN':
      if (Array.isArray(expect)) return !expect.includes(actual);
      return actual !== expect;
    case 'REGEX':
      if (!strActual || !expect) return false;
      try {
        return new RegExp(String(expect)).test(strActual);
      } catch {
        return false;
      }
  }
  return false;
}
```

**注意**：前端的 `match` 逻辑应与后端 `LinkageValidator.match()` 完全一致，避免前后端判断结果不一致。

### 10.6 边界情况处理

| 边界情况 | 建议处理 |
|----------|----------|
| 目标字段被删除 | 规则保留但标记为无效，或自动清理 |
| 触发字段被删除 | 条件永远不满足（触发字段值为 undefined） |
| 循环联动（A→B→A） | 配置时检测并禁止，或设置最大递归深度 |
| 规则 sortOrder 冲突 | 按规则ID稳定排序，或前端提示调整 |
| 条件树深度过大 | 限制最大嵌套层级（如 5 层） |
| 联动设置的值与用户输入冲突 | 被 DISABLED 的字段才强制设值；可用字段允许用户修改 |

---

## 附录 A：完整的联动规则 JSON 示例

```json
{
  "id": "rule-001",
  "formId": "form-abc",
  "version": "1",
  "name": "已婚显示配偶信息",
  "targetFieldId": "spouseInfo",
  "actionType": "SHOW",
  "actionValue": null,
  "enable": true,
  "sortOrder": 1,
  "createTime": "2026-05-21 10:00:00",
  "conditionTree": [
    {
      "id": "node-root",
      "ruleId": "rule-001",
      "formId": "form-abc",
      "version": "1",
      "parentId": null,
      "nodeType": "AND",
      "triggerFieldId": null,
      "triggerCondition": null,
      "triggerValue": null,
      "sortOrder": 0,
      "children": [
        {
          "id": "node-001",
          "ruleId": "rule-001",
          "formId": "form-abc",
          "version": "1",
          "parentId": "node-root",
          "nodeType": "CONDITION",
          "triggerFieldId": "isMarried",
          "triggerCondition": "EQ",
          "triggerValue": "true",
          "sortOrder": 0,
          "children": null
        }
      ]
    }
  ]
}
```

## 附录 B：与后端接口对接说明

### 获取联动规则

后端应在以下接口中返回联动规则：

- `GET /api/form/info/{formId}` — 获取表单详情（含规则）
- `GET /api/form/infoByVersion/{formId}/{version}` — 按版本获取表单详情（含规则）

返回的表单数据中，联动规则列表应放在 `linkageRules` 字段中，且每个规则的 `conditionTree` 已组装为树形结构。

### 规则保存

- `POST /api/form/save` — 保存表单（含联动规则）
- `PUT /api/form/update` — 更新表单（含联动规则）

前端提交时，联动规则作为表单配置的一部分提交，后端负责将条件树扁平化后存入 `cat_dynamic_form_linkage_node` 表。

---

> 本文档由后端代码（`LinkageValidator.java`、`DynamicFormLinkageRule.java`、`DynamicFormLinkageNode.java`）推导整理，如有实现调整请以最新代码为准。