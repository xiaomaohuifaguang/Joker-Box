# 动态表单 - 添加接口

> 作者：小猫会发光
> 日期：2026-05-09

## 基本信息

| 项 | 值 |
| --- | --- |
| 接口路径 | `/dynamicForm/add` |
| 请求方式 | `POST` |
| Content-Type | `application/json`（`@RequestBody`） |
| Service | `DynamicFormService#add` |
| 事务 | `@Transactional(rollbackFor = Exception.class)` |

> 创建表单定义，同时保存 DRAFT 版本的字段（含分组）、联动规则。

---

## 请求参数（DynamicForm）

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| name | String | 是 | 表单名称 |
| description | String | 否 | 描述 |
| groups | List<DynamicFormFieldGroup> | 否 | 字段分组（推荐） |
| formFields | List<DynamicFormField> | 否 | 平铺字段（无分组时用） |
| linkageRules | List<DynamicFormLinkageRule> | 否 | 联动规则 |

> `groups` 和 `formFields` 二选一，优先使用 `groups`。

### groups 结构（有分组时）

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| name | String | 是 | 分组名称 |
| description | String | 否 | 分组描述 |
| sort | Integer | 否 | 分组排序 |
| collapsed | String | 否 | `0` 展开 / `1` 折叠 |
| fields | List<DynamicFormField> | 否 | 分组下的字段 |

### DynamicFormField 字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| fieldId | String | 是 | 前端设计ID，表单内唯一 |
| title | String | 是 | 标题 |
| type | Enum | 是 | INPUT/NUMBER/SELECT/MULTISELECT/RADIO/CHECKBOX/DATE/DATETIME/TIME/SWITCH/TEXTAREA/UPLOAD/RATE/SLIDER/COLOR/CASCADER/MULTICASCADER |
| required | String | 否 | `1` 必填 |
| defaultValue | String | 否 | 默认值 |
| placeholder | String | 否 | 提示文案 |
| options | List | 否 | 选项（SELECT/RADIO/CHECKBOX/CASCADER 必填） |
| minLength | Integer | 否 | 最小长度 |
| maxLength | Integer | 否 | 最大长度 |
| min | Integer | 否 | 最小值（NUMBER） |
| max | Integer | 否 | 最大值（NUMBER） |
| pattern | String | 否 | 正则 |
| patternTips | String | 否 | 正则提示 |
| span | Integer | 否 | 宽度 1-24，默认24 |
| sort | Integer | 否 | 组内排序 |

---

## 请求示例

### 有分组的表单（推荐）

```http
POST /dynamicForm/add
Content-Type: application/json

{
  "name": "请假申请单",
  "description": "员工请假使用",
  "groups": [
    {
      "name": "基本信息",
      "description": "请填写申请人基本信息",
      "sort": 1,
      "collapsed": "0",
      "fields": [
        {
          "fieldId": "field_name",
          "title": "姓名",
          "type": "INPUT",
          "required": "1",
          "span": 12,
          "sort": 1
        },
        {
          "fieldId": "field_dept",
          "title": "部门",
          "type": "SELECT",
          "required": "1",
          "span": 12,
          "sort": 2,
          "options": [
            { "label": "技术部", "value": "tech" },
            { "label": "人事部", "value": "hr" }
          ]
        }
      ]
    },
    {
      "name": "请假明细",
      "sort": 2,
      "collapsed": "0",
      "fields": [
        {
          "fieldId": "field_type",
          "title": "请假类型",
          "type": "RADIO",
          "required": "1",
          "options": [
            { "label": "事假", "value": "personal" },
            { "label": "病假", "value": "sick" }
          ]
        },
        {
          "fieldId": "field_days",
          "title": "请假天数",
          "type": "NUMBER",
          "required": "1",
          "min": 0.5,
          "max": 30
        }
      ]
    }
  ],
  "linkageRules": [
    {
      "name": "病假显示医院证明",
      "targetFieldId": "field_hospital",
      "actionType": "SHOW",
      "enable": true,
      "conditionTree": [
        {
          "nodeType": "CONDITION",
          "triggerFieldId": "field_type",
          "triggerCondition": "EQ",
          "triggerValue": "sick"
        }
      ]
    }
  ]
}
```

### 无分组的表单（兼容）

```http
POST /dynamicForm/add
Content-Type: application/json

{
  "name": "简单表单",
  "formFields": [
    {
      "fieldId": "field_1",
      "title": "姓名",
      "type": "INPUT",
      "required": "1"
    }
  ]
}
```

---

## 响应数据（HttpResult<Void>）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | long | `200` 成功，`-1` 失败 |
| data | Void | 无业务数据 |

---

## 异常情况

| 触发条件 | 异常 |
| --- | --- |
| fieldId 重复 | `IllegalArgumentException("表单项 fieldId 存在重复")` |
| 表单项校验失败 | `IllegalArgumentException("表单项校验失败: {title}")` |
| 未登录 | `IllegalStateException("当前未登录")` |
