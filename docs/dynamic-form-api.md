# 动态表单系统 — 前后端对接文档

> 本文档基于后端代码和数据库结构整理，覆盖动态表单完整功能，供前端开发对接使用。
> 文档版本：2026-05-21

---

## 目录

1. [项目结构](#1-项目结构)
2. [数据库表结构](#2-数据库表结构)
3. [实体类定义](#3-实体类定义)
4. [字段类型枚举](#4-字段类型枚举)
5. [字段类型详解](#5-字段类型详解)
6. [联动规则](#6-联动规则)
7. [API 接口](#7-api-接口)
8. [版本管理](#8-版本管理)
9. [文件上传](#9-文件上传)
10. [数据流转完整示例](#10-数据流转完整示例)

---

## 1. 项目结构

### 1.1 模块划分

```
Joker-Box/
├── common/                              # 公共模块（实体、工具、枚举）
│   └── src/main/java/com/cat/common/
│       └── entity/dynamicForm/          # 动态表单实体类
│           ├── DynamicForm.java
│           ├── DynamicFormField.java
│           ├── DynamicFormFieldGroup.java
│           ├── DynamicFormFieldInstance.java
│           ├── DynamicFormFieldType.java
│           ├── DynamicFormInstance.java
│           ├── DynamicFormLinkageNode.java
│           ├── DynamicFormLinkageRule.java
│           ├── DynamicFormOption.java
│           └── FormData.java
│       └── handler/
│           ├── DefaultValueTypeHandler.java   # defaultValue/val 类型处理器
│           └── JsonValueTypeHandler.java      # triggerValue/actionValue 类型处理器
│
└── simple/                              # 业务模块（API、Service、Mapper）
    └── src/main/java/com/cat/simple/
        └── form/
            ├── controller/
            │   └── DynamicFormController.java   # REST API
            ├── service/
            │   ├── DynamicFormService.java
            │   └── impl/DynamicFormServiceImpl.java
            └── mapper/                        # MyBatis Mapper
        └── file/
            └── controller/
                └── FileController.java        # 文件上传/下载
```

### 1.2 数据库表（6张）

| 表名 | 说明 |
|------|------|
| `cat_dynamic_form` | 表单模板 |
| `cat_dynamic_form_field` | 字段定义 |
| `cat_dynamic_form_field_group` | 字段分组 |
| `cat_dynamic_form_instance` | 表单实例（提交记录） |
| `cat_dynamic_form_field_instance` | 字段实例值 |
| `cat_dynamic_form_linkage_rule` | 联动规则 |
| `cat_dynamic_form_linkage_node` | 联动条件节点（树形） |

---

## 2. 数据库表结构

### 2.1 cat_dynamic_form（表单模板）

```sql
CREATE TABLE `cat_dynamic_form` (
  `id` varchar(255) NOT NULL COMMENT '表单id',
  `name` varchar(255) COMMENT '表单名称',
  `description` varchar(255) COMMENT '描述',
  `version` varchar(255) NOT NULL DEFAULT '1' COMMENT '版本',
  `status` varchar(255) NOT NULL DEFAULT '0' COMMENT '状态 0草稿 1发布 -1停用',
  `deleted` varchar(255) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_by` varchar(255) COMMENT '创建人',
  `create_time` datetime COMMENT '创建时间',
  `update_time` datetime COMMENT '更新时间',
  PRIMARY KEY (`id`)
);
```

### 2.2 cat_dynamic_form_field（字段定义）

```sql
CREATE TABLE `cat_dynamic_form_field` (
  `id` varchar(255) NOT NULL COMMENT '表单项id',
  `form_id` varchar(255) COMMENT '表单id',
  `field_id` varchar(255) COMMENT '前端设计id',
  `group_id` varchar(255) COMMENT '所属分组id',
  `version` varchar(255) DEFAULT '1' COMMENT '版本',
  `title` varchar(255) COMMENT '标题',
  `type` varchar(255) COMMENT '类型',
  `required` varchar(255) COMMENT '必填 0/1',
  `default_value` varchar(255) COMMENT '默认值',
  `placeholder` varchar(255) COMMENT '提示',
  `options` text COMMENT '单选多选配置（JSON）',
  `min_length` int COMMENT '最小长度/最少上传数',
  `max_length` int COMMENT '最大长度/最多上传数',
  `min` int COMMENT '最小值/最少勾选数/最小行数',
  `max` int COMMENT '最大值/最多勾选数/最大行数',
  `pattern` varchar(255) COMMENT '正则表达式',
  `pattern_tips` varchar(255) COMMENT '正则提示',
  `span` int DEFAULT 1 COMMENT '宽度 1-24',
  `deleted` varchar(255) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_by` varchar(255) COMMENT '创建人',
  `create_time` datetime COMMENT '创建时间',
  `update_time` datetime COMMENT '更新时间',
  `sort` int COMMENT '排序',
  PRIMARY KEY (`id`)
);
```

### 2.3 cat_dynamic_form_field_group（字段分组）

```sql
CREATE TABLE `cat_dynamic_form_field_group` (
  `id` varchar(255) NOT NULL COMMENT '分组id',
  `form_id` varchar(255) COMMENT '表单id',
  `version` varchar(255) COMMENT '版本',
  `name` varchar(255) COMMENT '分组名称',
  `description` varchar(500) COMMENT '分组描述',
  `sort` int DEFAULT 0 COMMENT '排序',
  `collapsed` varchar(1) DEFAULT '0' COMMENT '0展开 1折叠',
  `deleted` varchar(255) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_by` varchar(255) COMMENT '创建人',
  `create_time` datetime COMMENT '创建时间',
  `update_time` datetime COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_form_version` (`form_id`, `version`)
);
```

### 2.4 cat_dynamic_form_instance（表单实例）

```sql
CREATE TABLE `cat_dynamic_form_instance` (
  `id` varchar(255) NOT NULL COMMENT '动态表单实例id',
  `form_id` varchar(255) COMMENT '表单id',
  `version` varchar(255) COMMENT '版本',
  `deleted` varchar(255) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_by` varchar(255) COMMENT '创建人',
  `create_time` datetime COMMENT '创建时间',
  `update_time` datetime COMMENT '更新时间',
  PRIMARY KEY (`id`)
);
```

### 2.5 cat_dynamic_form_field_instance（字段实例值）

```sql
CREATE TABLE `cat_dynamic_form_field_instance` (
  `id` varchar(255) NOT NULL COMMENT '表单项实例id',
  `form_field_id` varchar(255) COMMENT '表单项id',
  `form_instance_id` varchar(255) COMMENT '表单实例id',
  `version` varchar(255) COMMENT '版本',
  `val` text COMMENT '真实值（JSON）',
  `deleted` varchar(255) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_by` varchar(255) COMMENT '创建人',
  `create_time` datetime COMMENT '创建时间',
  `update_time` datetime COMMENT '更新时间',
  PRIMARY KEY (`id`)
);
```

### 2.6 cat_dynamic_form_linkage_rule（联动规则）

```sql
CREATE TABLE `cat_dynamic_form_linkage_rule` (
  `id` varchar(64) NOT NULL COMMENT '主键ID',
  `form_id` varchar(64) NOT NULL COMMENT '表单ID',
  `version` varchar(32) NOT NULL COMMENT '版本号',
  `name` varchar(128) COMMENT '规则名称',
  `target_field_id` varchar(64) NOT NULL COMMENT '目标字段 fieldId',
  `action_type` varchar(32) NOT NULL COMMENT '动作类型',
  `action_value` json COMMENT '动作参数（JSON）',
  `enable` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `deleted` varchar(1) DEFAULT '0' COMMENT '逻辑删除',
  `create_by` varchar(64) COMMENT '创建人',
  `create_time` datetime COMMENT '创建时间',
  `update_time` datetime COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_form_version` (`form_id`,`version`),
  KEY `idx_target` (`target_field_id`)
);
```

### 2.7 cat_dynamic_form_linkage_node（联动条件节点）

```sql
CREATE TABLE `cat_dynamic_form_linkage_node` (
  `id` varchar(64) NOT NULL COMMENT '主键ID',
  `rule_id` varchar(64) NOT NULL COMMENT '所属规则ID',
  `form_id` varchar(64) NOT NULL DEFAULT '' COMMENT '表单ID',
  `version` varchar(32) NOT NULL DEFAULT '' COMMENT '版本号',
  `parent_id` varchar(64) COMMENT '父节点ID，null=根节点',
  `node_type` varchar(16) NOT NULL COMMENT 'AND/OR/CONDITION',
  `trigger_field_id` varchar(64) COMMENT '触发字段 fieldId',
  `trigger_condition` varchar(32) COMMENT '条件运算符',
  `trigger_value` json COMMENT '触发值（JSON）',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `deleted` varchar(1) DEFAULT '0' COMMENT '逻辑删除',
  `create_by` varchar(64) COMMENT '创建人',
  `create_time` datetime COMMENT '创建时间',
  `update_time` datetime COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_rule_id` (`rule_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_form_version` (`form_id`, `version`)
);
```

---

## 3. 实体类定义

### 3.1 DynamicForm（表单模板）

| 字段 | 类型 | 数据库字段 | 说明 |
|------|------|-----------|------|
| id | String | id | 表单唯一ID（UUID） |
| name | String | name | 表单名称，最多64字符 |
| description | String | description | 描述，最多500字符 |
| version | String | version | 当前版本：DRAFT 或数字字符串 |
| status | String | status | 0草稿 / 1发布 / -1停用 |
| deleted | String | deleted | 逻辑删除 0/1 |
| createBy | String | create_by | 创建人ID |
| createByName | String | - | 创建人昵称（查询时关联） |
| createTime | LocalDateTime | create_time | 创建时间 |
| updateTime | LocalDateTime | update_time | 更新时间 |
| formFields | List<DynamicFormField> | - | 未分组字段（TableField(exist=false)） |
| groups | List<DynamicFormFieldGroup> | - | 字段分组（TableField(exist=false)） |
| linkageRules | List<DynamicFormLinkageRule> | - | 联动规则（TableField(exist=false)） |

### 3.2 DynamicFormField（字段定义）

| 字段 | 类型 | 数据库字段 | 说明 |
|------|------|-----------|------|
| id | String | id | 字段唯一ID（UUID） |
| fieldId | String | field_id | 前端设计ID，表单内唯一。格式：`^[a-zA-Z][a-zA-Z0-9_]{0,31}$` |
| formId | String | form_id | 所属表单ID |
| groupId | String | group_id | 所属分组ID，null=未分组 |
| version | String | version | 版本号 |
| title | String | title | 字段标题，最多32字符 |
| type | DynamicFormFieldType | type | 字段类型（枚举字符串） |
| required | String | required | "0"选填 / "1"必填 |
| defaultValue | Object | default_value | 默认值。String原样存，数组/对象走JSON |
| placeholder | String | placeholder | 占位提示，最多128字符 |
| options | List<DynamicFormOption> | options | 选项配置（Fastjson2TypeHandler序列化为JSON） |
| minLength | Integer | min_length | 最小长度（INPUT/TEXTAREA）或最少上传数（UPLOAD） |
| maxLength | Integer | max_length | 最大长度（INPUT/TEXTAREA）或最多上传数（UPLOAD） |
| min | Integer | min | 最小值/最少勾选数/最小行数/滑块最小值/评分最大值 |
| max | Integer | max | 最大值/最多勾选数/最大行数/滑块最大值 |
| pattern | String | pattern | 正则表达式 |
| patternTips | String | pattern_tips | 正则校验失败提示，最多64字符 |
| span | Integer | span | 宽度 1-24，默认24 |
| deleted | String | deleted | 逻辑删除 |
| createBy | String | create_by | 创建人 |
| createTime | LocalDateTime | create_time | 创建时间 |
| updateTime | LocalDateTime | update_time | 更新时间 |
| sort | Integer | sort | 排序，越小越靠前 |

### 3.3 DynamicFormOption（选项）

| 字段 | 类型 | 说明 |
|------|------|------|
| label | String | 显示文本 |
| value | String | 实际值 |
| children | List<DynamicFormOption> | 子选项（级联用） |

### 3.4 DynamicFormFieldGroup（字段分组）

| 字段 | 类型 | 数据库字段 | 说明 |
|------|------|-----------|------|
| id | String | id | 分组ID（UUID） |
| formId | String | form_id | 表单ID |
| version | String | version | 版本 |
| name | String | name | 分组名称，最多32字符 |
| description | String | description | 分组描述 |
| sort | Integer | sort | 排序 |
| collapsed | String | collapsed | 0展开 / 1折叠 |
| deleted | String | deleted | 逻辑删除 |
| createBy | String | create_by | 创建人 |
| createTime | LocalDateTime | create_time | 创建时间 |
| updateTime | LocalDateTime | update_time | 更新时间 |
| fields | List<DynamicFormField> | - | 分组下的字段（TableField(exist=false)） |

### 3.5 DynamicFormInstance（表单实例）

| 字段 | 类型 | 数据库字段 | 说明 |
|------|------|-----------|------|
| id | String | id | 实例ID（UUID） |
| formId | String | form_id | 表单ID |
| version | String | version | 版本 |
| deleted | String | deleted | 逻辑删除 |
| createBy | String | create_by | 创建人 |
| createTime | LocalDateTime | create_time | 创建时间 |
| updateTime | LocalDateTime | update_time | 更新时间 |

### 3.6 DynamicFormFieldInstance（字段实例值）

| 字段 | 类型 | 数据库字段 | 说明 |
|------|------|-----------|------|
| id | String | id | 字段实例ID（UUID） |
| formFieldId | String | form_field_id | 字段定义ID |
| formInstanceId | String | form_instance_id | 表单实例ID |
| version | String | version | 版本 |
| val | Object | val | 字段值。String原样存，数组/对象走JSON |
| deleted | String | deleted | 逻辑删除 |
| createBy | String | create_by | 创建人 |
| createTime | LocalDateTime | create_time | 创建时间 |
| updateTime | LocalDateTime | update_time | 更新时间 |

### 3.7 DynamicFormLinkageRule（联动规则）

| 字段 | 类型 | 数据库字段 | 说明 |
|------|------|-----------|------|
| id | String | id | 规则ID（UUID） |
| formId | String | form_id | 表单ID |
| version | String | version | 版本 |
| name | String | name | 规则名称 |
| targetFieldId | String | target_field_id | 目标字段 fieldId |
| actionType | String | action_type | 动作类型 |
| actionValue | Object | action_value | 动作参数（JsonValueTypeHandler） |
| enable | Boolean | enable | 是否启用 |
| sortOrder | Integer | sort_order | 排序 |
| deleted | String | deleted | 逻辑删除 |
| createBy | String | create_by | 创建人 |
| createTime | LocalDateTime | create_time | 创建时间 |
| updateTime | LocalDateTime | update_time | 更新时间 |
| conditionTree | List<DynamicFormLinkageNode> | - | 条件节点树（TableField(exist=false)） |

### 3.8 DynamicFormLinkageNode（联动条件节点）

| 字段 | 类型 | 数据库字段 | 说明 |
|------|------|-----------|------|
| id | String | id | 节点ID（UUID） |
| ruleId | String | rule_id | 所属规则ID |
| formId | String | form_id | 表单ID |
| version | String | version | 版本 |
| parentId | String | parent_id | 父节点ID，null=根节点 |
| nodeType | String | node_type | AND / OR / CONDITION |
| triggerFieldId | String | trigger_field_id | 触发字段 fieldId（CONDITION用） |
| triggerCondition | String | trigger_condition | 条件运算符（CONDITION用） |
| triggerValue | Object | trigger_value | 触发值（JsonValueTypeHandler，CONDITION用） |
| sortOrder | Integer | sort_order | 排序 |
| deleted | String | deleted | 逻辑删除 |
| createBy | String | create_by | 创建人 |
| createTime | LocalDateTime | create_time | 创建时间 |
| updateTime | LocalDateTime | update_time | 更新时间 |
| children | List<DynamicFormLinkageNode> | - | 子节点（TableField(exist=false)） |

### 3.9 FormData（表单数据提交）

| 字段 | 类型 | 说明 |
|------|------|------|
| formId | String | 表单模板ID |
| version | String | 版本号，留空则使用表单当前发布版本 |
| formInstanceId | String | 表单实例ID，首次提交传null，更新时传已有ID |
| data | Map<String, Object> | 字段值映射，key=fieldId，value=字段值 |

---

## 4. 字段类型枚举

```java
public enum DynamicFormFieldType {
    INPUT,          // 文本输入
    NUMBER,         // 数字
    SELECT,         // 下拉单选
    MULTISELECT,    // 下拉多选
    RADIO,          // 单选框
    CHECKBOX,       // 复选框
    DATE,           // 日期
    DATETIME,       // 日期时间
    TIME,           // 时间
    DATERANGE,      // 日期范围
    SWITCH,         // 开关
    TEXTAREA,       // 多行文本
    UPLOAD,         // 文件上传
    RATE,           // 评分
    SLIDER,         // 滑块
    COLOR,          // 颜色选择器
    CASCADER,       // 级联选择（单选）
    MULTICASCADER   // 级联选择（多选）
}
```

---

## 5. 字段类型详解

### 5.1 配置项速查表

| 字段类型 | options | min | max | minLength | maxLength | pattern |
|---------|---------|-----|-----|-----------|-----------|---------|
| INPUT | - | - | - | 字符串最小长度 | 字符串最大长度 | 支持 |
| NUMBER | - | 数值最小值 | 数值最大值 | - | - | - |
| SELECT | 必填 | - | - | - | - | - |
| MULTISELECT | 必填 | 最少选择数 | 最多选择数 | - | - | - |
| RADIO | 必填 | - | - | - | - | - |
| CHECKBOX | 必填 | 最少勾选数 | 最多勾选数 | - | - | - |
| DATE | - | - | - | - | - | - |
| DATETIME | - | - | - | - | - | - |
| TIME | - | - | - | - | - | - |
| DATERANGE | - | - | - | - | - | - |
| SWITCH | - | - | - | - | - | - |
| TEXTAREA | - | 最小行数 | 最大行数 | 字符串最小长度 | 字符串最大长度 | 支持 |
| UPLOAD | - | - | - | 最少上传数 | 最多上传数 | - |
| RATE | - | - | 最大分值 | - | - | - |
| SLIDER | - | 最小值 | 最大值 | - | - | - |
| COLOR | - | - | - | - | - | - |
| CASCADER | 必填 | - | - | - | - | - |
| MULTICASCADER | 必填 | 最少选择数 | 最多选择数 | - | - | - |

### 5.2 数据格式速查表

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
| DATERANGE | Array<String> | `["2026-05-01", "2026-05-21"]` |
| SWITCH | String | `"true"` / `"false"` |
| TEXTAREA | String | `"多行文本内容"` |
| UPLOAD | Array<Object> | `[{"id":"xxx","filename":"a.pdf","contentType":"application/pdf","size":1024}]` |
| RATE | String | `"4"` |
| SLIDER | String | `"50"` |
| COLOR | String | `"#1890ff"` |
| CASCADER | String | `"xihu"`（选中节点的 value） |
| MULTICASCADER | Array<String> | `["xihu", "gulou"]`（选中节点 value 的数组） |

### 5.3 各类型默认值校验

| 字段类型 | 默认值校验 |
|---------|-----------|
| INPUT | 字符串 |
| NUMBER | 必须是数字字符串 |
| SELECT / RADIO | 必须在 options 中 |
| MULTISELECT / CHECKBOX | 元素需在 options 中（未严格校验） |
| CASCADER / MULTICASCADER | 无严格校验 |
| SWITCH | `"true"` / `"false"` / `"1"` / `"0"` |
| RATE | 数字字符串，0 ~ max |
| SLIDER | 数字字符串，min ~ max |

---

## 6. 联动规则

### 6.1 规则结构

```json
{
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

### 6.2 动作类型（actionType）

| 动作 | actionValue 类型 | 说明 |
|------|-----------------|------|
| SHOW | null | 显示目标字段 |
| HIDE | null | 隐藏目标字段 |
| REQUIRED | Boolean | true=必填, false=选填 |
| DISABLED | Boolean | true=禁用 |
| ENABLED | Boolean | true=启用 |
| SET_PATTERN | String / Object | 设置正则。裸值 `"^[a-z]+$"` 或 `{"pattern": "^[a-z]+$"}` |
| SET_SPAN | Integer / Object | 设置宽度。裸值 `12` 或 `{"span": 12}` |
| OPTION | Array | 设置选项列表（替换 options） |
| VALUE | Any | 设置字段值 |

### 6.3 动作与字段类型兼容性

- 所有字段支持：SHOW, HIDE, REQUIRED, DISABLED, ENABLED, SET_SPAN, VALUE
- INPUT / TEXTAREA 额外支持：SET_PATTERN
- SELECT / MULTISELECT / RADIO / CHECKBOX / CASCADER / MULTICASCADER 额外支持：OPTION
- UPLOAD 不支持：VALUE, SET_PATTERN

### 6.4 条件节点类型（nodeType）

| 类型 | 说明 |
|------|------|
| AND | 所有子节点同时满足 |
| OR | 任一子节点满足 |
| CONDITION | 单个条件判断 |

### 6.5 条件运算符（triggerCondition）

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

### 6.6 复杂条件树示例

```json
{
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
  ]
}
```

含义：`amount > 1000 AND (expense_type = "travel" OR expense_type = "entertainment")`

---

## 7. API 接口

### 7.1 统一响应格式

```java
public class HttpResult<T> {
    private long code = 200;          // 状态码
    private T data;                    // 响应数据
    private String msg = "success";    // 响应消息
    private long timestamp;            // 时间戳
}
```

**成功响应：**
```json
{
  "code": 200,
  "msg": "success",
  "data": { ... },
  "timestamp": 1716259200000
}
```

**失败响应：**
```json
{
  "code": 500,
  "msg": "字段标题不能为空",
  "data": null,
  "timestamp": 1716259200000
}
```

### 7.2 表单模板管理接口

#### 7.2.1 创建表单

```
POST /dynamicForm/add
Content-Type: application/json
```

**请求体：** 完整的 DynamicForm JSON（含字段、分组、联动规则）

```json
{
  "name": "报销申请单",
  "description": "员工费用报销",
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
        }
      ]
    }
  ],
  "formFields": [],
  "linkageRules": []
}
```

**注意：**
- id 传 null，后端自动生成 UUID
- 创建后状态为 `0`（草稿），版本为 `DRAFT`
- 字段的 id、formId、version、createBy、createTime、updateTime 都不需要传

**响应：**
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

---

#### 7.2.2 删除表单

```
POST /dynamicForm/remove
Content-Type: application/json
```

**请求体：**
```json
{
  "id": "form-uuid"
}
```

**限制：** 仅草稿（status=0）或停用（status=-1）状态可删除

---

#### 7.2.3 修改表单

```
POST /dynamicForm/update
Content-Type: application/json
```

**请求体：** 完整的 DynamicForm JSON（同 add）

**注意：**
- 修改的是 DRAFT 版本的数据
- 已发布的数字版本不受影响
- 传 id 即可定位表单

---

#### 7.2.4 查询表单详情

```
POST /dynamicForm/info
Content-Type: application/json
```

**请求体：**
```json
{
  "id": "form-uuid",
  "version": "1"
}
```

**version 说明：**
- 表单 status=0（草稿）或 status=-1（停用）：返回 DRAFT 版本，`version` 参数无效
- 表单 status=1（发布）：返回指定 version 的数据，不传则返回当前发布版本

**响应：** 完整的 DynamicForm（含 groups、formFields、linkageRules）

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": "form-uuid",
    "name": "报销申请单",
    "version": "1",
    "status": "1",
    "groups": [
      {
        "id": "group-uuid",
        "name": "基本信息",
        "collapsed": "0",
        "fields": [ ... ]
      }
    ],
    "formFields": [ ... ],
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

---

#### 7.2.5 分页查询表单列表

```
POST /dynamicForm/queryPage
Content-Type: application/json
```

**请求体（PageParam）：**
```json
{
  "current": 1,
  "size": 10
}
```

**响应：**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": "form-uuid",
        "name": "报销申请单",
        "version": "2",
        "status": "1",
        "createBy": "user-id",
        "createByName": "张三",
        "createTime": "2026-05-01 10:00:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

#### 7.2.6 发布表单

```
POST /dynamicForm/deploy?formId=form-uuid
```

**说明：**
- 将 DRAFT 版本的数据复制为新的数字版本
- 首次发布版本号为 `"1"`，之后递增
- 发布后表单状态变为 `1`（发布）
- 发布前会做完整校验（字段配置、联动规则、选项等）

---

#### 7.2.7 停用表单

```
POST /dynamicForm/stop?formId=form-uuid
```

**说明：**
- 将表单状态变为 `-1`（停用）
- 将最新数字版本的数据复制回 DRAFT，可继续编辑

---

#### 7.2.8 已发布表单列表（含历史版本）

```
POST /dynamicForm/publishedForms
```

**说明：**
- 返回所有已发布表单及其历史版本列表，供流程绑定表单时使用
- 仅包含 `status=1`（已发布）的表单
- 每个表单附带所有发布过的历史版本（从发布历史表读取）

**请求：**

无需请求参数，body 为空即可。

```json
{}
```

**响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "formId": "form_main_001",
      "formName": "报销申请表",
      "latestVersion": "2",
      "versions": [
        { "version": "2", "publishTime": "2026-05-20T14:30:00" },
        { "version": "1", "publishTime": "2026-05-01T10:00:00" }
      ]
    },
    {
      "formId": "form_abc123",
      "formName": "采购审批表",
      "latestVersion": "3",
      "versions": [
        { "version": "3", "publishTime": "2026-05-22T09:00:00" },
        { "version": "2", "publishTime": "2026-05-10T16:00:00" },
        { "version": "1", "publishTime": "2026-04-28T11:00:00" }
      ]
    }
  ]
}
```

**字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `formId` | String | 表单唯一ID |
| `formName` | String | 表单名称 |
| `latestVersion` | String | 最新发布版本号 |
| `versions` | Array | 历史版本列表，按版本号降序排列 |
| `versions[].version` | String | 版本号 |
| `versions[].publishTime` | String | 发布时间（ISO 8601 格式） |

**前端交互建议：**

```
下拉框展示：报销申请表 (V2)
                    采购审批表 (V3)

选中后自动带入：formId=form_main_001, formVersion=2

[切换版本] → 弹窗/下拉展开历史版本
  → 选中 V1 → 替换 formVersion=1
```

---

### 7.3 表单数据提交接口

#### 7.3.1 提交/更新表单数据

```
POST /dynamicForm/submit
Content-Type: application/json
```

**请求体（FormData）：**

首次提交：
```json
{
  "formId": "form-uuid",
  "version": "",
  "formInstanceId": null,
  "data": {
    "applicant": "张三",
    "amount": "1500.50",
    "expense_type": "travel",
    "skills": ["java", "vue"],
    "is_urgent": "false",
    "attachments": [
      { "fileId": "file-uuid", "name": "合同.pdf", "size": 1048576, "type": "application/pdf" }
    ],
    "satisfaction": "4"
  }
}
```

更新提交：
```json
{
  "formId": "form-uuid",
  "version": "",
  "formInstanceId": "instance-uuid",
  "data": { ... }
}
```

**参数说明：**
- `formId`：表单模板ID（必填）
- `version`：版本号，留空则使用表单当前发布版本
- `formInstanceId`：首次提交传 null，更新时传已有实例ID
- `data`：字段值映射，key = fieldId，value = 字段值

**后端校验逻辑：**
1. 检查表单是否存在且已发布
2. 根据版本加载字段定义
3. 计算联动规则效果（hidden/disabled）
4. 跳过 hidden/disabled 字段的校验
5. 按字段类型做必填、长度、范围、正则校验
6. 保存/更新表单实例和字段实例值

---

### 7.4 文件上传接口

#### 7.4.1 上传文件

```
POST /file/uploadDynamicForm
Content-Type: multipart/form-data

FormData:
  - uploadFile: File (二进制文件)
```

**响应：**
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

#### 7.4.2 下载文件

```
GET /file/downloadDynamicForm?fileId=file-uuid
```

直接返回文件流，前端可用 `<a>` 标签或 `window.open` 触发下载。

---

## 8. 版本管理

### 8.1 状态流转

```
创建表单 → [草稿status=0] → 发布 → [版本1 status=1] → 发布 → [版本2 status=1]
                            ↓
                          停用 → [草稿status=-1]（回退到最新版本数据）
```

### 8.2 状态说明

| 状态 | 值 | 说明 |
|------|-----|------|
| 草稿 | 0 | 可编辑，对外不可见，数据存储为 DRAFT 版本 |
| 发布 | 1 | 已发布，数据存储为数字版本（1,2,3...），不可直接编辑 |
| 停用 | -1 | 已停用，对外不可见，数据回退为 DRAFT 版本可继续编辑 |

### 8.3 版本规则

- **草稿阶段**：所有数据版本号为 `"DRAFT"`
- **发布时**：将 DRAFT 数据复制为新的数字版本（首次 `"1"`，之后递增）
- **发布后编辑**：修改会覆盖 DRAFT 版本，不影响已发布的数字版本
- **停用后**：将最新数字版本的数据复制回 DRAFT，可以继续编辑

### 8.4 各接口的版本行为

| 接口 | 版本行为 |
|------|---------|
| add | 创建 DRAFT 版本 |
| update | 覆盖 DRAFT 版本 |
| info（草稿/停用） | 返回 DRAFT 版本 |
| info（发布） | 返回指定 version，默认当前发布版本 |
| deploy | DRAFT → 新数字版本 |
| stop | 数字版本 → DRAFT，status=-1 |
| submit（version为空） | 使用表单当前发布版本 |
| submit（version指定） | 使用指定版本 |

---

## 9. 文件上传

### 9.1 UPLOAD 字段 val 格式

```json
[
  {
    "id": "file-uuid-1",
    "filename": "合同.pdf",
    "size": 1048576,
    "contentType": "application/pdf"
  },
  {
    "id": "file-uuid-2",
    "filename": "发票.jpg",
    "size": 204800,
    "contentType": "image/jpeg"
  }
]
```

### 9.2 交互流程

```
用户选择文件
    ↓
前端调 POST /file/uploadDynamicForm 上传
    ↓
后端返回 FileInfo {id, filename, contentType, size}
    ↓
前端提取 {fileId, name, size, type} 追加到 val 数组
    ↓
表单提交时 val 随 data 一起提交
    ↓
用户点击文件名 → 调 GET /file/downloadDynamicForm?fileId=xxx 下载
```

### 9.3 校验规则

- 必填时：空数组 `[]` 算未填
- `minLength`：数组长度 >= minLength（最少上传几个）
- `maxLength`：数组长度 <= maxLength（最多上传几个）

---

## 10. 数据流转完整示例

### 10.1 场景：创建一个报销单并提交数据

#### Step 1：创建表单模板

```
POST /dynamicForm/add
```

```json
{
  "name": "员工报销单",
  "description": "",
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
            { "label": "办公费", "value": "office" }
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
            { "label": "高铁", "value": "train" }
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
        }
      ]
    }
  ],
  "formFields": [],
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

#### Step 2：发布表单

```
POST /dynamicForm/deploy?formId={返回的formId}
```

发布后表单状态变为 `1`，版本变为 `"1"`。

#### Step 3：查询表单详情（渲染用）

```
POST /dynamicForm/info
```

```json
{
  "id": "form-uuid"
}
```

返回完整表单结构，前端据此渲染表单。

#### Step 4：用户填写并提交

```
POST /dynamicForm/submit
```

```json
{
  "formId": "form-uuid",
  "version": "",
  "formInstanceId": null,
  "data": {
    "applicant": "张三",
    "apply_date": "2026-05-21",
    "expense_type": "travel",
    "transport": "plane",
    "amount": "1500"
  }
}
```

#### Step 5：再次提交（更新）

```
POST /dynamicForm/submit
```

```json
{
  "formId": "form-uuid",
  "version": "",
  "formInstanceId": "instance-uuid",
  "data": {
    "applicant": "张三",
    "apply_date": "2026-05-21",
    "expense_type": "travel",
    "transport": "train",
    "amount": "2000"
  }
}
```

---

## 附录：前端渲染建议

### A. 整体渲染流程

```
1. 调 /dynamicForm/info 获取表单模板
2. 解析 groups 和 formFields，按 sort 排序
3. 渲染分组（含折叠控制）和未分组字段
4. 初始化各字段的默认值
5. 绑定联动规则监听（字段值变化时重新计算规则）
6. 用户填写后，收集 data 调 /dynamicForm/submit
```

### B. 联动规则实时计算伪代码

```javascript
function onFieldChange(fieldId, value) {
  data[fieldId] = value;

  const sortedRules = linkageRules.sort((a, b) => a.sortOrder - b.sortOrder);

  for (const rule of sortedRules) {
    if (!rule.enable) continue;
    const matched = evaluateConditionTree(rule.conditionTree, data);
    if (matched) {
      applyAction(rule.targetFieldId, rule.actionType, rule.actionValue);
    }
  }
}

function evaluateConditionTree(nodes, data) {
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
    return compare(data[node.triggerFieldId], node.triggerCondition, node.triggerValue);
  }
  return false;
}
```

### C. 提交前前端校验

1. 遍历所有字段
2. 跳过被联动规则隐藏（HIDE）或禁用（DISABLED）的字段
3. 检查必填：数组类型判 `length === 0`，字符串判 `trim() === ''`
4. 检查长度/数量限制：单值字段判字符串长度，数组字段判元素个数
5. 检查数值范围（NUMBER/SLIDER/RATE）
6. 检查正则匹配（INPUT/TEXTAREA）

### D. 编辑回显

1. 调 `/dynamicForm/info` 获取表单模板
2. 根据 `formInstanceId` 查询实例数据（后端目前没有单独的查询实例详情接口，需要前端或后端补充）
3. 将各字段实例的 `val` 回填到对应 `fieldId` 的组件中

---

> 本文档基于后端代码 `master` 分支（2026-05-21）整理。如有疑问请与后端对齐。
