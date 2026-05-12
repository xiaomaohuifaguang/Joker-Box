# 动态表单字段联动规则 - 接口文档

> 作者：小猫会发光
> 日期：2026-05-08
> 版本：v2（树形嵌套条件）

## 概述

联动规则用于控制表单字段之间的显隐、必填、禁用等状态。支持**任意嵌套的条件组合**，如 `(A AND (B OR C))`。

联动规则跟随表单版本管理，通过 `/dynamicForm/add`、`/dynamicForm/update`、`/dynamicForm/info` 接口整体维护。

---

## 数据结构

### DynamicFormLinkageRule（规则）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | String | 否 | 规则ID，新增时传 null |
| name | String | 否 | 规则名称，前端展示用 |
| targetFieldId | String | **是** | 目标字段 fieldId（被控制的字段） |
| actionType | String | **是** | 动作类型，见下表 |
| actionValue | Object | 否 | 动作参数，JSON格式 |
| enable | Boolean | 否 | 是否启用，默认 true |
| sortOrder | Integer | 否 | 规则执行顺序，越小越优先 |
| conditionTree | List<DynamicFormLinkageNode> | **是** | 条件节点树，根节点为 AND/OR |

### DynamicFormLinkageNode（条件节点）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | String | 否 | 节点ID，新增时传 null |
| nodeType | String | **是** | `AND` / `OR` / `CONDITION` |
| triggerFieldId | String | CONDITION 时必填 | 触发字段 fieldId |
| triggerCondition | String | CONDITION 时必填 | 条件运算符，见下表 |
| triggerValue | Object | CONDITION 时必填 | 触发值 |
| sortOrder | Integer | 否 | 同级排序 |
| children | List<DynamicFormLinkageNode> | AND/OR 时必填 | 子节点列表 |

### 动作类型（actionType）

| 值 | 说明 | actionValue |
|----|------|------------|
| SHOW | 显示目标字段 | 无 |
| HIDE | 隐藏目标字段 | 无 |
| REQUIRED | 设为必填 | `true` / `false`，默认 true |
| DISABLED | 设为禁用 | `true` / `false`，默认 true |
| ENABLED | 设为可用 | 无 |
| SET_PATTERN | 设置正则校验 | `{"pattern": "^\\d+$", "patternTips": "只能输入数字"}` |
| SET_SPAN | 设置宽度 | `{"span": 12}` |
| OPTION | 设置选项列表 | 选项数组 |
| VALUE | 设置字段值 | 具体值 |

### 条件运算符（triggerCondition）

| 值 | 说明 | triggerValue 类型 |
|----|------|------------------|
| EQ | 等于 | 字符串/数字/布尔 |
| NE | 不等于 | 字符串/数字/布尔 |
| GT | 大于 | 数字 |
| LT | 小于 | 数字 |
| GE | 大于等于 | 数字 |
| LE | 小于等于 | 数字 |
| IN | 包含于 | 数组 |
| NOT_IN | 不包含于 | 数组 |
| EMPTY | 为空 | 无（忽略 triggerValue） |
| NOT_EMPTY | 非空 | 无（忽略 triggerValue） |
| REGEX | 正则匹配 | 正则字符串 |

---

## 接口说明

联动规则**不单独提供增删改查接口**，作为 `DynamicForm` 对象的子对象，通过以下接口整体操作：

| 接口 | 路径 | 说明 |
|------|------|------|
| 添加表单 | `POST /dynamicForm/add` | 整体保存表单 + 字段 + 联动规则 |
| 修改表单 | `POST /dynamicForm/update` | 整体覆盖保存 DRAFT 版本的字段和联动规则 |
| 表单详情 | `POST /dynamicForm/info` | 返回表单 + 字段 + 联动规则（conditionTree 已组装为树） |
| 提交表单 | `POST /dynamicForm/submit` | 提交数据时自动执行联动规则判定 |

---

## 请求示例

### 示例1：单条件（兼容存量）

当 `type` 等于 `"其他"` 时，显示 `remark` 字段：

```json
{
  "name": "测试表单",
  "formFields": [
    { "fieldId": "type", "title": "类型", "type": "SELECT", "required": "1" },
    { "fieldId": "remark", "title": "备注", "type": "TEXTAREA" }
  ],
  "linkageRules": [
    {
      "name": "其他类型显示备注",
      "targetFieldId": "remark",
      "actionType": "SHOW",
      "sortOrder": 0,
      "conditionTree": [
        {
          "nodeType": "AND",
          "children": [
            {
              "nodeType": "CONDITION",
              "triggerFieldId": "type",
              "triggerCondition": "EQ",
              "triggerValue": "其他"
            }
          ]
        }
      ]
    }
  ]
}
```

### 示例2：多条件 AND

当 `dept` 等于 `"技术部"` **且** `level` 大于等于 `3` 时，`salary` 字段必填：

```json
{
  "linkageRules": [
    {
      "name": "技术部高级员工必填薪资",
      "targetFieldId": "salary",
      "actionType": "REQUIRED",
      "conditionTree": [
        {
          "nodeType": "AND",
          "children": [
            {
              "nodeType": "CONDITION",
              "triggerFieldId": "dept",
              "triggerCondition": "EQ",
              "triggerValue": "技术部"
            },
            {
              "nodeType": "CONDITION",
              "triggerFieldId": "level",
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

### 示例3：嵌套条件（AND + OR）

当 `dept` 等于 `"技术部"` **且**（`level` 大于等于 `3` **或** `city` 属于 `["北京", "上海"]`）时，显示 `salary` 字段：

```json
{
  "linkageRules": [
    {
      "name": "技术部高阶或一线城市显示薪资",
      "targetFieldId": "salary",
      "actionType": "SHOW",
      "conditionTree": [
        {
          "nodeType": "AND",
          "children": [
            {
              "nodeType": "CONDITION",
              "triggerFieldId": "dept",
              "triggerCondition": "EQ",
              "triggerValue": "技术部"
            },
            {
              "nodeType": "OR",
              "children": [
                {
                  "nodeType": "CONDITION",
                  "triggerFieldId": "level",
                  "triggerCondition": "GE",
                  "triggerValue": 3
                },
                {
                  "nodeType": "CONDITION",
                  "triggerFieldId": "city",
                  "triggerCondition": "IN",
                  "triggerValue": ["北京", "上海"]
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

### 示例4：多规则影响同一字段

多条规则可以控制同一个 `targetFieldId`，按 `sortOrder` 顺序执行，后执行的覆盖先执行的：

```json
{
  "linkageRules": [
    {
      "name": "类型为A时显示",
      "targetFieldId": "extra",
      "actionType": "SHOW",
      "sortOrder": 0,
      "conditionTree": [{ "nodeType": "AND", "children": [
        { "nodeType": "CONDITION", "triggerFieldId": "type", "triggerCondition": "EQ", "triggerValue": "A" }
      ]}]
    },
    {
      "name": "类型为B时隐藏（覆盖上一条）",
      "targetFieldId": "extra",
      "actionType": "HIDE",
      "sortOrder": 1,
      "conditionTree": [{ "nodeType": "AND", "children": [
        { "nodeType": "CONDITION", "triggerFieldId": "type", "triggerCondition": "EQ", "triggerValue": "B" }
      ]}]
    }
  ]
}
```

---

## 响应示例（info 接口）

```json
{
  "code": 200,
  "data": {
    "id": "form001",
    "name": "测试表单",
    "version": "DRAFT",
    "status": "0",
    "formFields": [...],
    "linkageRules": [
      {
        "id": "rule001",
        "name": "其他类型显示备注",
        "targetFieldId": "remark",
        "actionType": "SHOW",
        "enable": true,
        "sortOrder": 0,
        "conditionTree": [
          {
            "id": "node001",
            "nodeType": "AND",
            "children": [
              {
                "id": "node002",
                "nodeType": "CONDITION",
                "triggerFieldId": "type",
                "triggerCondition": "EQ",
                "triggerValue": "其他"
              }
            ]
          }
        ]
      }
    ]
  }
}
```

---

## 联动执行逻辑

1. 表单提交时，后端按 `sortOrder` 升序遍历所有 `linkageRules`
2. 跳过 `enable = false` 的规则
3. 对每条规则，递归判定 `conditionTree`：
   - `CONDITION` 节点：根据 `triggerCondition` 比较字段值与 `triggerValue`
   - `AND` 节点：所有子节点都为 true 时才为 true（短路）
   - `OR` 节点：任一子节点为 true 时就为 true（短路）
4. 条件成立则对 `targetFieldId` 执行 `actionType`
5. 被 `HIDE` 或 `DISABLED` 的字段跳过必填校验
6. `SET_PATTERN` 和 `SET_SPAN` 会覆盖字段原始配置参与校验

---

## 注意事项

1. **根节点统一为 AND**：为了数据结构统一，每条规则的 `conditionTree` 根节点固定为 `AND` 或 `OR`，即使只有单条件也包一层根节点
2. **新增时 ID 传 null**：rule 和 node 的 `id` 在新增时传 `null`，后端自动生成 UUID
3. **修改即覆盖**：`/dynamicForm/update` 会物理删除 DRAFT 版本的所有 rule 和 node，然后重新插入传入的数据
4. **版本跟随表单**：发布/停用表单时，联动规则随表单版本一起复制
