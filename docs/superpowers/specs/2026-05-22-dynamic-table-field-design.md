# 动态表格（TABLE）字段类型设计

## 1. 概述

在动态表单系统中新增 `TABLE` 字段类型，支持可增删行的动态表格。列结构由表单设计者定义（纯文本列），行由填表人动态增删。

### 核心特征

- **列结构固定**：设计者定义列的 key 和 title，所有单元格为纯文本输入
- **行动态增删**：填表人可以添加或删除行
- **行约束**：支持最少/最多行数、必填
- **默认值**：支持预设默认行数据
- **联动支持**：作为触发字段支持 EMPTY/NOT_EMPTY，作为目标字段支持 SHOW/HIDE/REQUIRED/DISABLED/ENABLED/SET_SPAN

---

## 2. 数据模型

### 2.1 新增枚举值

`DynamicFormFieldType` 新增 `TABLE`。

### 2.2 新增 DynamicFormTableColumn 实体

```java
package com.cat.common.entity.dynamicForm;

public class DynamicFormTableColumn implements Serializable {
    private String key;    // 列标识，如 "name"
    private String title;  // 列标题，如 "姓名"
}
```

列顺序由数组顺序决定，无需额外 sort 字段。

### 2.3 DynamicFormField 新增 columns 字段

```java
@Schema(description = "动态表格列定义（仅 TABLE 类型使用）")
@TableField(typeHandler = Fastjson2TypeHandler.class)
private List<DynamicFormTableColumn> columns;
```

### 2.4 数据库 DDL

```sql
ALTER TABLE cat_dynamic_form_field ADD columns TEXT COMMENT '动态表格列定义（仅TABLE类型）';
```

### 2.5 提交值结构

行对象数组，存入现有 `DynamicFormFieldInstance.val`（TEXT 列），DefaultValueTypeHandler 自动序列化/反序列化。

```json
[
  { "name": "张三", "age": "25", "phone": "138xxxx" },
  { "name": "李四", "age": "30", "phone": "139xxxx" }
]
```

### 2.6 defaultValue 结构

与提交值结构一致，行对象数组：

```json
[
  { "name": "", "age": "", "phone": "" }
]
```

---

## 3. 发布前校验

`validateField()` 新增 `TABLE` 分支：

| 校验项 | 规则 |
|--------|------|
| columns 非空 | TABLE 类型必须有至少一列 |
| 列 key | 必填，格式 `^[a-zA-Z][a-zA-Z0-9_]{0,31}$`，同一字段内不重复 |
| 列 title | 必填，长度不超过 32 字符 |
| min | 不能为负数 |
| max | 不能小于 1；未设置时表示不限制行数上限 |
| min vs max | max 不能小于 min |
| defaultValue | 每行必须是对象，对象 key 必须在 columns 中定义（多余 key 报错，缺少 key 不报错） |

---

## 4. 表单提交校验

`validateFormData()` 新增 `TABLE` 分支：

| 校验项 | 规则 |
|--------|------|
| 必填 | required=1 时行数不能为 0；当 min > 0 时 required 校验自然满足，不重复报错 |
| 最少行数 | min 不为空时行数 >= min |
| 最多行数 | max 不为空时行数 <= max；max 未设置时不限制行数上限 |
| 行数据格式 | 每行必须是 Map |
| 行数据 key | 每行的 key 必须在 columns 中定义 |
| 列值 | 不做深度校验（纯文本） |

---

## 5. 联动支持

### 5.1 TABLE 作为触发字段

仅支持 EMPTY 和 NOT_EMPTY，其他条件类型对 TABLE 无意义，前端联动编辑器不展示。

| 条件类型 | 支持情况 | 说明 |
|---------|---------|------|
| EMPTY | 支持 | 行数组为空时触发 |
| NOT_EMPTY | 支持 | 行数组非空时触发 |

### 5.2 TABLE 作为目标字段

| 动作类型 | 支持情况 | 说明 |
|---------|---------|------|
| SHOW / HIDE | 支持 | 显隐整个表格 |
| REQUIRED | 支持 | 设置表格是否必填 |
| DISABLED / ENABLED | 支持 | 禁用/启用整个表格 |
| SET_SPAN | 支持 | 设置宽度 |
| VALUE | 不支持 | 设置整个表格行数据场景极少且复杂 |
| OPTION | 不支持 | TABLE 无选项概念 |
| SET_PATTERN | 不支持 | 纯文本列不需要正则 |

`validateActionCompatibility` 中 TABLE 类型的合法动作集合与 UPLOAD 一致：`SHOW, HIDE, REQUIRED, DISABLED, ENABLED, SET_SPAN`。

---

## 6. 前端交互约定

### 6.1 字段定义结构（前端提交给后端）

```json
{
  "fieldId": "members",
  "title": "人员列表",
  "type": "TABLE",
  "required": "1",
  "min": 1,
  "max": 10,
  "columns": [
    { "key": "name", "title": "姓名" },
    { "key": "age", "title": "年龄" },
    { "key": "phone", "title": "电话" }
  ],
  "defaultValue": [
    { "name": "", "age": "", "phone": "" }
  ]
}
```

### 6.2 提交值结构

```json
[
  { "name": "张三", "age": "25", "phone": "138xxxx" },
  { "name": "李四", "age": "30", "phone": "139xxxx" }
]
```

### 6.3 前后端分工

- **后端**：列定义校验、行约束校验、行数据 key 合法性校验
- **前端**：列内单元格渲染为纯文本 input、行增删交互、列值内容不做深度校验