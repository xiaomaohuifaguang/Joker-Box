# 动态表单系统功能说明文档

> 本文档描述后端现有动态表单系统的完整功能，供前端开发对齐使用。
> 基于代码版本：master 分支（2026-05-21）

---

## 目录

1. [概述](#1-概述)
2. [核心概念](#2-核心概念)
3. [数据模型](#3-数据模型)
4. [字段类型详解](#4-字段类型详解)
5. [字段通用属性](#5-字段通用属性)
6. [字段分组](#6-字段分组)
7. [联动规则](#7-联动规则)
8. [表单数据提交](#8-表单数据提交)
9. [文件上传](#9-文件上传)
10. [API 接口](#10-api-接口)
11. [版本管理](#11-版本管理)
12. [前端渲染建议](#12-前端渲染建议)

---

## 1. 概述

动态表单系统支持：
- 通过 JSON 配置表单模板（字段、校验、联动）
- 表单版本管理（草稿 → 发布 → 停用）
- 运行时渲染表单并收集数据
- 字段级联动规则（显示/隐藏/必填/禁用/选项/值等）
- 字段分组与布局控制

---

## 2. 核心概念

| 概念 | 说明 |
|------|------|
| **表单模板（DynamicForm）** | 表单定义，包含字段、分组、联动规则 |
| **字段定义（DynamicFormField）** | 表单中的一个输入项，含类型、校验、默认值等 |
| **字段分组（DynamicFormFieldGroup）** | 将字段按业务分组展示，支持折叠 |
| **表单实例（DynamicFormInstance）** | 用户提交一次表单产生的记录 |
| **字段实例（DynamicFormFieldInstance）** | 某个实例中某个字段的具体值 |
| **联动规则（DynamicFormLinkageRule）** | 当条件满足时，对目标字段执行某动作 |
| **版本（Version）** | DRAFT（草稿）或数字版本（1, 2, 3...） |

---

## 3. 数据模型

### 3.1 表单模板（DynamicForm）

```json
{
  "id": "form-uuid",
  "name": "报销申请单",
  "description": "员工费用报销",
  "version": "1",
  "status": "1",
  "createBy": "user-id",
  "createTime": "2026-05-21 10:00:00",
  "updateTime": "2026-05-21 10:00:00",
  "formFields": [...],
  "groups": [...],
  "linkageRules": [...]
}
```

**字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 表单唯一ID |
| name | String | 表单名称（必填，最多64字符） |
| description | String | 描述（最多500字符） |
| version | String | 当前版本号：DRAFT 或数字字符串 |
| status | String | 0草稿 / 1发布 / -1停用 |
| formFields | Array | 未分组字段列表 |
| groups | Array | 字段分组列表 |
| linkageRules | Array | 联动规则列表 |

### 3.2 字段定义（DynamicFormField）

```json
{
  "id": "field-uuid",
  "fieldId": "expense_type",
  "formId": "form-uuid",
  "groupId": "group-uuid",
  "version": "1",
  "title": "费用类型",
  "type": "SELECT",
  "required": "1",
  "defaultValue": "travel",
  "placeholder": "请选择费用类型",
  "options": [
    { "label": "差旅费", "value": "travel" },
    { "label": "办公费", "value": "office" }
  ],
  "minLength": null,
  "maxLength": null,
  "min": null,
  "max": null,
  "pattern": null,
  "patternTips": null,
  "span": 12,
  "sort": 0
}
```

### 3.3 选项结构（DynamicFormOption）

```json
{
  "label": "显示文本",
  "value": "实际值",
  "children": [
    { "label": "子选项", "value": "child" }
  ]
}
```

### 3.4 字段分组（DynamicFormFieldGroup）

```json
{
  "id": "group-uuid",
  "formId": "form-uuid",
  "version": "1",
  "name": "基本信息",
  "description": "员工基本信息",
  "sort": 0,
  "collapsed": "0",
  "fields": [...]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 分组ID |
| name | String | 分组名称（必填，最多32字符） |
| description | String | 分组描述 |
| sort | Integer | 排序，越小越靠前 |
| collapsed | String | 0展开 / 1折叠 |
| fields | Array | 该分组下的字段列表 |

### 3.5 联动规则（DynamicFormLinkageRule）

```json
{
  "id": "rule-uuid",
  "formId": "form-uuid",
  "version": "1",
  "name": "费用类型为差旅时显示交通方式",
  "targetFieldId": "transport",
  "actionType": "SHOW",
  "actionValue": null,
  "enable": true,
  "sortOrder": 0,
  "conditionTree": [
    {
      "nodeType": "CONDITION",
      "triggerFieldId": "expense_type",
      "triggerCondition": "EQ",
      "triggerValue": "travel"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 规则名称 |
| targetFieldId | String | 目标字段的 fieldId |
| actionType | String | 动作类型 |
| actionValue | Object | 动作参数，视 actionType 而定 |
| enable | Boolean | 是否启用 |
| sortOrder | Integer | 排序 |
| conditionTree | Array | 条件节点树（根节点列表） |

**actionType 枚举：**

| 动作 | actionValue 类型 | 说明 |
|------|-----------------|------|
| SHOW | null | 显示目标字段 |
| HIDE | null | 隐藏目标字段 |
| REQUIRED | Boolean | true=必填, false=选填 |
| DISABLED | Boolean | true=禁用 |
| ENABLED | Boolean | true=启用 |
| SET_PATTERN | String / Object{pattern} | 设置正则表达式 |
| SET_SPAN | Integer / Object{span} | 设置字段宽度（1-24） |
| OPTION | Array | 设置选项列表（替换 options） |
| VALUE | Any | 设置字段值 |

**动作与字段类型的兼容性：**

- 所有字段支持：SHOW, HIDE, REQUIRED, DISABLED, ENABLED, SET_SPAN, VALUE
- INPUT / TEXTAREA 额外支持：SET_PATTERN
- SELECT / MULTISELECT / RADIO / CHECKBOX / CASCADER / MULTICASCADER 额外支持：OPTION
- UPLOAD 不支持：VALUE, SET_PATTERN

### 3.6 条件节点（DynamicFormLinkageNode）

```json
{
  "nodeType": "AND",
  "children": [
    {
      "nodeType": "CONDITION",
      "triggerFieldId": "amount",
      "triggerCondition": "GT",
      "triggerValue": 1000
    },
    {
      "nodeType": "OR",
      "children": [
        {
          "nodeType": "CONDITION",
          "triggerFieldId": "expense_type",
          "triggerCondition": "EQ",
          "triggerValue": "travel"
        },
        {
          "nodeType": "CONDITION",
          "triggerFieldId": "expense_type",
          "triggerCondition": "EQ",
          "triggerValue": "entertainment"
        }
      ]
    }
  ]
}
```

**节点类型（nodeType）：**

| 类型 | 说明 |
|------|------|
| AND | 所有子节点条件同时满足 |
| OR | 任一子节点条件满足 |
| CONDITION | 单个条件判断 |

**条件运算符（triggerCondition）：**

| 运算符 | 说明 | triggerValue 类型 |
|--------|------|-------------------|
| EQ | 等于 | 字符串/数字 |
| NE | 不等于 | 字符串/数字 |
| GT | 大于 | 数字 |
| LT | 小于 | 数字 |
| GE | 大于等于 | 数字 |
| LE | 小于等于 | 数字 |
| IN | 在数组中 | 数组 |
| NOT_IN | 不在数组中 | 数组 |
| EMPTY | 为空 | null |
| NOT_EMPTY | 不为空 | null |
| REGEX | 正则匹配 | 正则字符串 |

**注：** GT/LT/GE/LE 的 triggerValue 必须是数字；REGEX 的 triggerValue 必须是合法正则。

---

## 4. 字段类型详解

### 4.1 INPUT（文本输入）

**前端组件：** Input 输入框

**配置项：**
- `minLength` / `maxLength`：字符串长度限制
- `pattern`：正则表达式校验
- `patternTips`：正则校验失败提示
- `placeholder`：占位提示

**数据格式：** 字符串

**默认值：** 字符串

**示例：**
```json
{
  "fieldId": "username",
  "title": "用户名",
  "type": "INPUT",
  "required": "1",
  "minLength": 2,
  "maxLength": 20,
  "pattern": "^[a-zA-Z0-9_]+$",
  "patternTips": "用户名只能包含字母、数字和下划线",
  "placeholder": "请输入用户名"
}
```

---

### 4.2 NUMBER（数字输入）

**前端组件：** InputNumber 或带数字键盘的输入框

**配置项：**
- `min` / `max`：数值范围
- `placeholder`：占位提示

**数据格式：** 字符串（后端解析为 double）

**默认值：** 数字字符串（如 `"100.5"`）

**示例：**
```json
{
  "fieldId": "amount",
  "title": "金额",
  "type": "NUMBER",
  "required": "1",
  "min": 0,
  "max": 999999
}
```

---

### 4.3 SELECT（下拉单选）

**前端组件：** Select 下拉选择器

**配置项：**
- `options`：选项列表（必填）
- `placeholder`：占位提示

**数据格式：** 字符串（选中项的 value）

**默认值：** 必须是 options 中某个 value

**示例：**
```json
{
  "fieldId": "department",
  "title": "所属部门",
  "type": "SELECT",
  "required": "1",
  "options": [
    { "label": "研发部", "value": "rd" },
    { "label": "产品部", "value": "product" }
  ]
}
```

---

### 4.4 MULTISELECT（下拉多选）

**前端组件：** Select mode="multiple"

**配置项：**
- `options`：选项列表（必填）
- `min`：最少选择几项
- `max`：最多选择几项
- `placeholder`：占位提示

**数据格式：** 字符串数组（如 `["rd", "product"]`）

**默认值：** 数组，元素需在 options 中

**校验规则：**
- 必填时：空数组 `[]` 算未填
- `min`：数组长度 >= min
- `max`：数组长度 <= max

**示例：**
```json
{
  "fieldId": "skills",
  "title": "技能标签",
  "type": "MULTISELECT",
  "required": "1",
  "options": [
    { "label": "Java", "value": "java" },
    { "label": "Vue", "value": "vue" },
    { "label": "React", "value": "react" }
  ],
  "min": 1,
  "max": 3
}
```

---

### 4.5 RADIO（单选框）

**前端组件：** Radio.Group

**配置项：**
- `options`：选项列表（必填）

**数据格式：** 字符串（选中项的 value）

**默认值：** 必须是 options 中某个 value

**示例：**
```json
{
  "fieldId": "gender",
  "title": "性别",
  "type": "RADIO",
  "required": "1",
  "options": [
    { "label": "男", "value": "male" },
    { "label": "女", "value": "female" }
  ]
}
```

---

### 4.6 CHECKBOX（复选框）

**前端组件：** Checkbox.Group

**配置项：**
- `options`：选项列表（必填）
- `min`：最少勾选几项
- `max`：最多勾选几项

**数据格式：** 字符串数组（如 `["java", "vue"]`）

**默认值：** 数组，元素需在 options 中

**校验规则：** 同 MULTISELECT

**示例：**
```json
{
  "fieldId": "hobbies",
  "title": "兴趣爱好",
  "type": "CHECKBOX",
  "required": "0",
  "options": [
    { "label": "阅读", "value": "reading" },
    { "label": "运动", "value": "sports" },
    { "label": "旅行", "value": "travel" }
  ],
  "min": 1,
  "max": 2
}
```

---

### 4.7 DATE（日期选择）

**前端组件：** DatePicker

**配置项：**
- `placeholder`：占位提示

**数据格式：** 字符串（建议格式：`yyyy-MM-dd`）

**默认值：** 日期字符串

**示例：**
```json
{
  "fieldId": "birthday",
  "title": "出生日期",
  "type": "DATE",
  "required": "1"
}
```

---

### 4.8 DATETIME（日期时间选择）

**前端组件：** DatePicker showTime

**数据格式：** 字符串（建议格式：`yyyy-MM-dd HH:mm:ss`）

**示例：**
```json
{
  "fieldId": "meeting_time",
  "title": "会议时间",
  "type": "DATETIME",
  "required": "1"
}
```

---

### 4.9 TIME（时间选择）

**前端组件：** TimePicker

**数据格式：** 字符串（建议格式：`HH:mm:ss`）

**示例：**
```json
{
  "fieldId": "work_start",
  "title": "上班时间",
  "type": "TIME",
  "required": "1"
}
```

---

### 4.10 SWITCH（开关）

**前端组件：** Switch

**数据格式：** 字符串（`"true"` / `"false"` / `"1"` / `"0"`）

**默认值：** `"false"` 或 `"true"`

**示例：**
```json
{
  "fieldId": "is_urgent",
  "title": "是否紧急",
  "type": "SWITCH",
  "required": "0",
  "defaultValue": "false"
}
```

---

### 4.11 TEXTAREA（多行文本）

**前端组件：** Input.TextArea

**配置项：**
- `minLength` / `maxLength`：字符串长度限制
- `min` / `max`：最小/最大行数
- `pattern`：正则表达式
- `placeholder`：占位提示

**数据格式：** 字符串

**示例：**
```json
{
  "fieldId": "remark",
  "title": "备注说明",
  "type": "TEXTAREA",
  "required": "0",
  "minLength": 10,
  "maxLength": 500,
  "min": 3,
  "max": 10,
  "placeholder": "请详细描述"
}
```

---

### 4.12 UPLOAD（文件上传）

**前端组件：** Upload

**配置项：**
- `minLength`：最少上传数量
- `maxLength`：最多上传数量

**数据格式：** 文件对象数组

```json
[
  {
    "fileId": "file-uuid-1",
    "name": "合同.pdf",
    "size": 1048576,
    "type": "application/pdf"
  },
  {
    "fileId": "file-uuid-2",
    "name": "发票.jpg",
    "size": 204800,
    "type": "image/jpeg"
  }
]
```

**默认值：** 不支持（或空数组）

**校验规则：**
- 必填时：空数组 `[]` 算未填
- `minLength`：数组长度 >= minLength（最少上传几个）
- `maxLength`：数组长度 <= maxLength（最多上传几个）

**交互流程：**
1. 用户选择文件
2. 前端调 `POST /file/uploadDynamicForm` 上传
3. 后端返回 `FileInfo`
4. 前端提取 `{fileId, name, size, type}` 追加到 val 数组
5. 表单提交时 val 随 data 一起提交
6. 点击文件名下载：`GET /file/downloadDynamicForm?fileId=xxx`

**示例：**
```json
{
  "fieldId": "attachments",
  "title": "附件上传",
  "type": "UPLOAD",
  "required": "1",
  "minLength": 1,
  "maxLength": 5
}
```

---

### 4.13 RATE（评分）

**前端组件：** Rate

**配置项：**
- `max`：最大分值（如 5）

**数据格式：** 字符串（数字字符串，如 `"3"`）

**默认值：** 数字字符串，范围 0 ~ max

**示例：**
```json
{
  "fieldId": "satisfaction",
  "title": "满意度评分",
  "type": "RATE",
  "required": "1",
  "max": 5,
  "defaultValue": "5"
}
```

---

### 4.14 SLIDER（滑块）

**前端组件：** Slider

**配置项：**
- `min`：最小值
- `max`：最大值（必须 > min）

**数据格式：** 字符串（数字字符串）

**默认值：** 数字字符串，范围 min ~ max

**示例：**
```json
{
  "fieldId": "progress",
  "title": "完成进度",
  "type": "SLIDER",
  "required": "0",
  "min": 0,
  "max": 100,
  "defaultValue": "0"
}
```

---

### 4.15 COLOR（颜色选择器）

**前端组件：** ColorPicker

**数据格式：** 字符串（如 `"#1890ff"`）

**示例：**
```json
{
  "fieldId": "theme_color",
  "title": "主题颜色",
  "type": "COLOR",
  "required": "0"
}
```

---

### 4.16 CASCADER（级联选择 - 单选）

**前端组件：** Cascader

**配置项：**
- `options`：级联选项树（必填，支持嵌套，最多10层）

**数据格式：** 字符串（建议用逗号分隔的完整路径，如 `"zhejiang,hangzhou,xihu"`，具体格式由前端决定）

**默认值：** 合法路径值

**示例：**
```json
{
  "fieldId": "region",
  "title": "所在地区",
  "type": "CASCADER",
  "required": "1",
  "options": [
    {
      "label": "浙江省",
      "value": "zhejiang",
      "children": [
        {
          "label": "杭州市",
          "value": "hangzhou",
          "children": [
            { "label": "西湖区", "value": "xihu" }
          ]
        }
      ]
    }
  ]
}
```

---

### 4.17 MULTICASCADER（级联选择 - 多选）

**前端组件：** Cascader multiple

**配置项：**
- `options`：级联选项树（必填）
- `min`：最少选择几项
- `max`：最多选择几项

**数据格式：** 字符串数组（每个元素是一条完整路径）

**校验规则：**
- 必填时：空数组 `[]` 算未填
- `min`：数组长度 >= min
- `max`：数组长度 <= max

**示例：**
```json
{
  "fieldId": "service_areas",
  "title": "服务覆盖区域",
  "type": "MULTICASCADER",
  "required": "0",
  "min": 1,
  "max": 5,
  "options": [...]
}
```

---

## 5. 字段通用属性

所有字段共有的属性：

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | String | 后端生成 | 字段唯一ID |
| fieldId | String | 是 | 前端设计ID，表单内唯一。格式：`^[a-zA-Z][a-zA-Z0-9_]{0,31}$` |
| formId | String | 后端填充 | 所属表单ID |
| groupId | String | 否 | 所属分组ID，null 表示未分组 |
| version | String | 后端填充 | 版本号 |
| title | String | 是 | 字段标题，最多32字符 |
| type | Enum | 是 | 字段类型 |
| required | String | 否 | "0"选填（默认）/ "1"必填 |
| defaultValue | String | 否 | 默认值 |
| placeholder | String | 否 | 占位提示，最多128字符 |
| options | Array | 视类型 | 选项配置 |
| minLength | Integer | 否 | 最小长度（INPUT/TEXTAREA）或最少上传数（UPLOAD） |
| maxLength | Integer | 否 | 最大长度（INPUT/TEXTAREA）或最多上传数（UPLOAD） |
| min | Integer | 否 | 最小值/最少勾选数/最小行数/滑块最小值/评分最大值 |
| max | Integer | 否 | 最大值/最多勾选数/最大行数/滑块最大值 |
| pattern | String | 否 | 正则表达式 |
| patternTips | String | 否 | 正则校验失败提示，最多64字符 |
| span | Integer | 否 | 字段宽度（1-24），默认24，即占满一行 |
| sort | Integer | 否 | 排序，越小越靠前 |

---

## 6. 字段分组

表单字段可以按业务分组展示，分组支持折叠。

**数据结构：**
```json
{
  "id": "group-uuid",
  "name": "费用明细",
  "description": "请填写各项费用",
  "sort": 0,
  "collapsed": "0",
  "fields": [...]
}
```

**渲染规则：**
- 分组按 `sort` 升序排列
- 分组名称展示为分组标题
- `collapsed` = "1" 时默认折叠，"0" 时展开
- 分组内的字段按 `sort` 升序排列
- 未分组字段（groupId = null）直接展示在表单中

---

## 7. 联动规则

### 7.1 概述

联动规则用于实现字段间的动态交互。当条件满足时，对目标字段执行指定动作。

**执行时机：** 表单数据变化时实时计算（前端）+ 提交时后端二次校验。

### 7.2 规则结构

```json
{
  "name": "规则名称",
  "targetFieldId": "目标字段的fieldId",
  "actionType": "SHOW",
  "actionValue": null,
  "enable": true,
  "conditionTree": [
    {
      "nodeType": "AND",
      "children": [
        {
          "nodeType": "CONDITION",
          "triggerFieldId": "amount",
          "triggerCondition": "GT",
          "triggerValue": 1000
        },
        {
          "nodeType": "CONDITION",
          "triggerFieldId": "expense_type",
          "triggerCondition": "EQ",
          "triggerValue": "travel"
        }
      ]
    }
  ]
}
```

### 7.3 条件树求值

条件节点有三种类型，递归求值：

- **AND 节点**：所有子节点同时为 true，结果为 true
- **OR 节点**：任一子节点为 true，结果为 true
- **CONDITION 节点**：对 triggerFieldId 的当前值和 triggerValue 做比较

### 7.4 条件运算符说明

| 运算符 | 适用场景 | 示例 |
|--------|---------|------|
| EQ | 等于 | `triggerValue = "travel"`，字段值 = "travel" → true |
| NE | 不等于 | `triggerValue = "travel"`，字段值 = "office" → true |
| GT | 大于（数字比较） | `triggerValue = 1000`，字段值 = 1500 → true |
| LT | 小于（数字比较） | `triggerValue = 1000`，字段值 = 500 → true |
| GE | 大于等于 | `triggerValue = 18`，字段值 = 18 → true |
| LE | 小于等于 | `triggerValue = 18`，字段值 = 17 → true |
| IN | 在集合中 | `triggerValue = ["a","b"]`，字段值 = "a" → true |
| NOT_IN | 不在集合中 | `triggerValue = ["a","b"]`，字段值 = "c" → true |
| EMPTY | 为空 | 字段值为 null / "" / [] → true |
| NOT_EMPTY | 不为空 | 字段值有内容 → true |
| REGEX | 正则匹配 | `triggerValue = "^1[3-9]\\d{9}$"`，字段值 = "13800138000" → true |

**前端实现建议：**
- 所有字段值统一转字符串后比较（数字也先 toString）
- IN/NOT_IN 的 triggerValue 是数组，字段值如果是数组也支持
- EMPTY 判断：null、undefined、空字符串、空数组都算空

### 7.5 动作效果

| 动作 | 前端表现 | 对提交校验的影响 |
|------|---------|----------------|
| SHOW | 显示字段 | 正常校验 |
| HIDE | 隐藏字段 | 跳过该校验 |
| REQUIRED | 显示必填标记 | 按 actionValue（true/false）决定是否必填 |
| DISABLED | 禁用输入 | 跳过该校验 |
| ENABLED | 启用输入 | 正常校验 |
| SET_PATTERN | 更新正则规则 | 使用新正则校验 |
| SET_SPAN | 更新字段宽度 | 仅影响布局 |
| OPTION | 替换选项列表 | 用新选项做校验 |
| VALUE | 设置字段值 | 覆盖用户输入值后校验 |

**多条规则作用于同一字段时的优先级：**
- 如果多个规则的条件同时满足，以最后一条规则的效果为准（按 sortOrder 排序后处理）
- 建议前端在数据变化时重新计算所有规则，按 sortOrder 排序后依次应用

---

## 8. 表单数据提交

### 8.1 提交格式（FormData）

```json
{
  "formId": "form-uuid",
  "version": "1",
  "formInstanceId": null,
  "data": {
    "username": "张三",
    "amount": "1500.50",
    "expense_type": "travel",
    "skills": ["java", "vue"],
    "is_urgent": "false",
    "attachments": [
      { "fileId": "f1", "name": "合同.pdf", "size": 1024, "type": "application/pdf" }
    ],
    "satisfaction": "4",
    "region": "zhejiang,hangzhou,xihu"
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| formId | String | 表单模板ID |
| version | String | 版本号，留空则使用表单当前发布版本 |
| formInstanceId | String | 表单实例ID，首次提交传 null，更新时传已有实例ID |
| data | Object | 字段值映射，key = fieldId，value = 字段值 |

### 8.2 提交接口

```
POST /dynamicForm/submit
Content-Type: application/json

Body: FormData
```

### 8.3 各类型字段的 data 值格式

| 字段类型 | data 中的 value 类型 | 示例 |
|----------|---------------------|------|
| INPUT | String | `"hello"` |
| NUMBER | String | `"123.45"` |
| SELECT | String | `"option1"` |
| MULTISELECT | Array<String> | `["a", "b"]` |
| RADIO | String | `"male"` |
| CHECKBOX | Array<String> | `["reading", "sports"]` |
| DATE | String | `"2026-05-21"` |
| DATETIME | String | `"2026-05-21 14:30:00"` |
| TIME | String | `"14:30:00"` |
| SWITCH | String | `"true"` / `"false"` |
| TEXTAREA | String | `"多行文本内容"` |
| UPLOAD | Array<Object> | `[{"fileId":"xxx","name":"a.pdf","size":1024,"type":"pdf"}]` |
| RATE | String | `"4"` |
| SLIDER | String | `"50"` |
| COLOR | String | `"#1890ff"` |
| CASCADER | String | `"zhejiang,hangzhou,xihu"` |
| MULTICASCADER | Array<String> | `["zhejiang,hangzhou", "jiangsu,nanjing"]` |

---

## 9. 文件上传

### 9.1 上传接口

```
POST /file/uploadDynamicForm
Content-Type: multipart/form-data

FormData:
  - uploadFile: File (二进制文件)
```

**响应示例：**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": "file-uuid",
    "filename": "合同.pdf",
    "contentType": "application/pdf",
    "type": "file",
    "size": 1048576
  }
}
```

### 9.2 下载接口

```
GET /file/downloadDynamicForm?fileId={fileId}
```

直接返回文件流，前端可用 `<a>` 标签或 window.open 触发下载。

### 9.3 表单中 UPLOAD 字段的 val 格式

文件上传完成后，前端将返回的 `FileInfo` 提取核心字段，存入 data：

```json
[
  {
    "fileId": "file-uuid",
    "name": "合同.pdf",
    "size": 1048576,
    "type": "application/pdf"
  }
]
```

| 字段 | 说明 |
|------|------|
| fileId | 文件唯一ID，用于下载 |
| name | 文件名，前端展示用 |
| size | 文件大小（字节），前端展示用 |
| type | MIME 类型，前端判断文件类型图标用 |

### 9.4 交互建议

1. 用户选择文件后立即上传，显示上传进度
2. 上传成功后，在组件中展示文件列表（文件名 + 大小 + 删除按钮）
3. 点击文件名调用下载接口
4. 删除文件时，从 val 数组中移除对应项（文件暂不从服务器删除，等表单实例删除时统一清理）
5. 超出 `maxLength` 限制时禁止继续上传

---

## 10. API 接口

### 10.1 表单模板管理

| 接口 | 方法 | 说明 | 请求体 |
|------|------|------|--------|
| `/dynamicForm/add` | POST | 创建表单 | DynamicForm（含字段、分组、联动规则） |
| `/dynamicForm/remove` | POST | 删除表单（仅草稿/停用状态可删） | `{ "id": "form-id" }` |
| `/dynamicForm/update` | POST | 修改表单（仅草稿/停用状态可改） | DynamicForm（完整结构） |
| `/dynamicForm/info` | POST | 查询表单详情 | `{ "id": "form-id", "version": "1" }` |
| `/dynamicForm/queryPage` | POST | 分页查询表单列表 | PageParam |
| `/dynamicForm/deploy` | POST | 发布表单 | `formId` (form param) |
| `/dynamicForm/stop` | POST | 停用表单 | `formId` (form param) |

### 10.2 表单数据提交

| 接口 | 方法 | 说明 | 请求体 |
|------|------|------|--------|
| `/dynamicForm/submit` | POST | 提交/更新表单数据 | FormData |

### 10.3 文件上传

| 接口 | 方法 | 说明 |
|------|------|------|
| `/file/uploadDynamicForm` | POST | 动态表单文件上传 |
| `/file/downloadDynamicForm` | GET | 动态表单文件下载 |

### 10.4 info 接口响应示例

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": "form-uuid",
    "name": "报销申请单",
    "description": "",
    "version": "1",
    "status": "1",
    "groups": [
      {
        "id": "group-uuid",
        "name": "基本信息",
        "collapsed": "0",
        "fields": [...]
      }
    ],
    "formFields": [...],
    "linkageRules": [
      {
        "name": "规则1",
        "targetFieldId": "transport",
        "actionType": "SHOW",
        "actionValue": null,
        "enable": true,
        "conditionTree": [
          {
            "nodeType": "CONDITION",
            "triggerFieldId": "expense_type",
            "triggerCondition": "EQ",
            "triggerValue": "travel"
          }
        ]
      }
    ]
  }
}
```

**注意：**
- 表单状态为 `0`（草稿）或 `-1`（停用）时，info 返回 DRAFT 版本数据
- 表单状态为 `1`（发布）时，info 返回指定 version 的数据（默认当前发布版本）

---

## 11. 版本管理

### 11.1 状态流转

```
创建表单 → [草稿] → 发布 → [版本1] → 发布 → [版本2]
                    ↓
                  停用 → [草稿]（回退到最新发布版本）
```

### 11.2 状态说明

| 状态 | 值 | 说明 |
|------|-----|------|
| 草稿 | 0 | 可编辑，对外不可见，数据存储为 DRAFT 版本 |
| 发布 | 1 | 已发布，数据存储为数字版本（1, 2, 3...），不可直接编辑 |
| 停用 | -1 | 已停用，对外不可见，数据回退为 DRAFT 版本可继续编辑 |

### 11.3 版本规则

- **草稿阶段**：所有数据（字段、分组、联动规则）版本号为 `"DRAFT"`
- **发布时**：将 DRAFT 数据复制为新的数字版本（首次发布为 "1"，之后递增）
- **发布后编辑**：修改会覆盖 DRAFT 版本，不影响已发布的数字版本
- **停用后**：将最新数字版本的数据复制回 DRAFT，可以继续编辑

### 11.4 提交时的版本行为

- 提交时 `version` 字段留空：使用表单当前发布版本
- 提交时指定 `version`：使用指定版本（仅发布状态支持）
- 首次提交：`formInstanceId` 传 null，后端创建新实例
- 更新提交：`formInstanceId` 传已有实例ID，后端更新该实例的所有字段值

---

## 12. 前端渲染建议

### 12.1 整体渲染流程

```
1. 调 /dynamicForm/info 获取表单模板
2. 解析 groups 和 formFields，按 sort 排序
3. 渲染分组（含折叠控制）和未分组字段
4. 初始化各字段的默认值
5. 绑定联动规则监听（字段值变化时重新计算规则）
6. 用户填写后，收集 data 调 /dynamicForm/submit
```

### 12.2 布局渲染

- 每个字段占 `span / 24` 的宽度
- 建议一行默认24，即一列；两个字段可各设 span=12 实现两列布局
- 分组标题可展示为折叠面板（Collapse）或卡片标题

### 12.3 联动规则实时计算

**建议实现：**

```javascript
// 当任意字段值变化时
function onFieldChange(fieldId, value) {
  data[fieldId] = value;

  // 按 sortOrder 排序后依次计算每条规则
  const sortedRules = linkageRules.sort((a, b) => a.sortOrder - b.sortOrder);

  for (const rule of sortedRules) {
    if (!rule.enable) continue;

    const matched = evaluateConditionTree(rule.conditionTree, data);
    if (matched) {
      applyAction(rule.targetFieldId, rule.actionType, rule.actionValue);
    }
  }
}

// 条件树求值
function evaluateConditionTree(nodes, data) {
  // 根节点按 AND 逻辑处理（通常只有一个根节点）
  return nodes.every(node => evaluateNode(node, data));
}

function evaluateNode(node, data) {
  if (node.nodeType === 'AND') {
    return node.children.every(child => evaluateNode(child, data));
  }
  if (node.nodeType === 'OR') {
    return node.children.some(child => evaluateNode(child, data));
  }
  if (node.nodeType === 'CONDITION') {
    const fieldValue = data[node.triggerFieldId];
    return compare(fieldValue, node.triggerCondition, node.triggerValue);
  }
  return false;
}
```

**动作应用建议：**

| 动作 | 前端状态变量 | 说明 |
|------|-------------|------|
| SHOW/HIDE | `visible` | 控制字段显示/隐藏 |
| REQUIRED | `required` | 控制必填标记 |
| DISABLED/ENABLED | `disabled` | 控制禁用状态 |
| SET_PATTERN | `pattern` | 更新正则规则 |
| SET_SPAN | `span` | 更新布局宽度 |
| OPTION | `options` | 替换选项列表 |
| VALUE | `value` | 设置字段值（覆盖用户输入） |

**重要：** 隐藏（HIDE）和禁用（DISABLED）的字段，在提交时后端会跳过该校验。但前端仍需要把这些字段的值传给后端（传 null 或当前值均可），后端会根据联动效果决定是否校验。

### 12.4 表单提交前校验

前端应在提交前做一次完整校验，减少无效请求：

1. 遍历所有字段
2. 跳过被联动规则隐藏（HIDE）或禁用（DISABLED）的字段
3. 检查必填：数组类型判空用 `length === 0`，字符串类型判空用 `trim() === ''`
4. 检查长度/数量限制：单值字段判字符串长度，数组字段判元素个数
5. 检查数值范围（NUMBER/SLIDER/RATE）
6. 检查正则匹配（INPUT/TEXTAREA）
7. 检查选项合法性（SELECT/RADIO/CHECKBOX 等）

### 12.5 编辑回显

表单实例编辑时：

1. 先调 `/dynamicForm/info` 获取表单模板
2. 根据 `formInstanceId` 查询该实例的所有 `DynamicFormFieldInstance`
3. 将各字段实例的 `val` 回填到对应 `fieldId` 的组件中
4. 注意：字段定义可能已更新（发布了新版本），但实例数据是提交时的版本，以实例数据为准

---

## 附录：完整示例

### 表单模板配置示例

```json
{
  "name": "员工报销单",
  "description": "请如实填写报销信息",
  "groups": [
    {
      "name": "基本信息",
      "collapsed": "0",
      "sort": 0,
      "fields": [
        {
          "fieldId": "applicant",
          "title": "申请人",
          "type": "INPUT",
          "required": "1",
          "placeholder": "请输入姓名",
          "span": 12,
          "sort": 0
        },
        {
          "fieldId": "apply_date",
          "title": "申请日期",
          "type": "DATE",
          "required": "1",
          "span": 12,
          "sort": 1
        }
      ]
    },
    {
      "name": "费用明细",
      "collapsed": "0",
      "sort": 1,
      "fields": [
        {
          "fieldId": "expense_type",
          "title": "费用类型",
          "type": "SELECT",
          "required": "1",
          "options": [
            { "label": "差旅费", "value": "travel" },
            { "label": "办公费", "value": "office" },
            { "label": "招待费", "value": "entertainment" }
          ],
          "sort": 0
        },
        {
          "fieldId": "transport",
          "title": "交通方式",
          "type": "SELECT",
          "required": "0",
          "options": [
            { "label": "飞机", "value": "plane" },
            { "label": "高铁", "value": "train" },
            { "label": "汽车", "value": "car" }
          ],
          "sort": 1
        },
        {
          "fieldId": "amount",
          "title": "金额",
          "type": "NUMBER",
          "required": "1",
          "min": 0,
          "sort": 2
        },
        {
          "fieldId": "invoice_files",
          "title": "发票附件",
          "type": "UPLOAD",
          "required": "1",
          "maxLength": 5,
          "sort": 3
        }
      ]
    }
  ],
  "formFields": [
    {
      "fieldId": "remark",
      "title": "备注",
      "type": "TEXTAREA",
      "required": "0",
      "maxLength": 500,
      "placeholder": "其他补充说明",
      "sort": 0
    }
  ],
  "linkageRules": [
    {
      "name": "差旅费显示交通方式",
      "targetFieldId": "transport",
      "actionType": "SHOW",
      "actionValue": null,
      "enable": true,
      "sortOrder": 0,
      "conditionTree": [
        {
          "nodeType": "CONDITION",
          "triggerFieldId": "expense_type",
          "triggerCondition": "EQ",
          "triggerValue": "travel"
        }
      ]
    }
  ]
}
```

---

> 本文档基于后端代码实现整理，如有疑问请与后端对齐。