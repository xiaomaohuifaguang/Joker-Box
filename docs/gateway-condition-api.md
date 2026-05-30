# 网关流向条件配置接口文档

## 一、概述

本文档描述流程网关流向条件的配置方式，供前端流程设计器与后端对接使用。

**核心概念**：
- 条件绑定在 **sequenceFlow**（连线）上，不是绑定在网关节点上
- 支持 **NATIVE**（原生 JUEL 表达式）和 **CUSTOM**（可视化规则引擎）双模式
- 同一网关内两种模式可以混用
- 支持配置**默认走向**（所有条件都不满足时的兜底路径）

---

## 二、数据模型

### 2.1 条件配置头 `ProcessGatewayCondition`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | Long | — | 数据库自增ID，新增时不需要传 |
| `processDefinitionId` | Integer | **是** | 流程定义ID |
| `version` | String | **是** | 版本号：`"DRAFT"` / `"1"` / `"2"` |
| `sequenceFlowId` | String | **是** | BPMN 中 sequenceFlow 的 id，如 `"flow_1"` |
| `sourceNodeId` | String | **是** | 源节点ID（如网关ID或UserTask ID） |
| `targetNodeId` | String | **是** | 目标节点ID |
| `conditionType` | String | **是** | 条件类型：`"NATIVE"` / `"CUSTOM"` |
| `isDefault` | Boolean | 否 | 是否为默认走向：`true` / `false`，默认 `false` |
| `nativeExpression` | String | 条件 | NATIVE 模式时的 JUEL 表达式，如 `"${amount > 1000}"` |
| `ruleTree` | Object | 条件 | CUSTOM 模式时的规则树，见 2.2 节。根节点对象，不是数组 |

**规则**：
- `isDefault = true` 时：`conditionType` 后端存 `null`，前端可不传；`nativeExpression`、`ruleTree` 均可不传
- `isDefault = false` 且 `conditionType = "NATIVE"` 时：`nativeExpression` **必填**
- `isDefault = false` 且 `conditionType = "CUSTOM"` 时：`ruleTree` **必填**

---

### 2.2 规则树节点 `ProcessGatewayConditionNode`

CUSTOM 模式使用规则树表达条件逻辑，支持 AND/OR 嵌套。

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | Long | — | 数据库自增ID，新增时前端不需要传 |
| `conditionId` | Long | **是** | 所属条件配置ID，后端自动生成 |
| `parentId` | Long | 否 | 父节点ID，`null` 表示根节点，后端根据递归层级关联，前端不传 |
| `nodeType` | String | **是** | 节点类型：`"AND"` / `"OR"` / `"CONDITION"` |
| `category` | String | 条件 | CONDITION 节点必填，条件来源分类 |
| `fieldKey` | String | 条件 | CONDITION 节点必填，字段标识 |
| `operator` | String | 条件 | CONDITION 节点必填，运算符 |
| `value` | String | 条件 | CONDITION 节点必填，比较值（JSON字符串） |
| `sort` | Integer | 否 | 前端传入，同级排序，用于拖动调整顺序 |
| `children` | List | 否 | 子节点列表，AND/OR 节点必填 |

**节点类型说明**：

| nodeType | 说明 | 必填字段 |
|---|---|---|
| `"AND"` | 所有子节点条件同时满足 | `children` |
| `"OR"` | 任一子节点条件满足即可 | `children` |
| `"CONDITION"` | 单条比较条件 | `category`、`fieldKey`、`operator`、`value` |

---

### 2.3 条件来源分类 `category`

| category | 说明 | 取值示例 |
|---|---|---|
| `"FORM_FIELD"` | 表单字段值 | 表单中定义的 `fieldId`，如 `"amount"`、`"leaveType"` |
| `"HANDLER_DEPT"` | 当前处理人部门 | 当前登录用户所属的部门ID列表 |
| `"HANDLER_ROLE"` | 当前处理人角色 | 当前登录用户拥有的角色ID列表 |
| `"PREV_HANDLER_DEPT"` | 上一级处理人部门 | 上一节点所有处理人的部门ID列表（去重） |
| `"PREV_HANDLER_ROLE"` | 上一级处理人角色 | 上一节点所有处理人的角色ID列表（去重） |

---

### 2.4 运算符 `operator`

| operator | 含义 | 适用类型 | value 示例 |
|---|---|---|---|
| `"EQ"` | 等于 | 任意 | `"1000"` / `"personal"` |
| `"NE"` | 不等于 | 任意 | `"1000"` |
| `"GT"` | 大于 | 数值 | `"1000"` |
| `"LT"` | 小于 | 数值 | `"1000"` |
| `"GE"` | 大于等于 | 数值 | `"1000"` |
| `"LE"` | 小于等于 | 数值 | `"1000"` |
| `"IN"` | 在列表中 | 任意 | `"[\"personal\",\"sick\"]"`（必须是 JSON 数组字符串，元素为字符串类型） |
| `"NOT_IN"` | 不在列表中 | 任意 | `"[\"admin\",\"manager\"]"`（必须是 JSON 数组字符串，元素为字符串类型） |
| `"EMPTY"` | 为空 | 字符串/集合 | — |
| `"NOT_EMPTY"` | 不为空 | 字符串/集合 | — |
| `"REGEX"` | 正则匹配 | 字符串 | `"^[A-Z]{2}-\\d{4}$"` |

**注意**：
- `EMPTY` / `NOT_EMPTY` 的 `value` 可以传空字符串或 `null`
- `IN` / `NOT_IN` 的 `value` 必须是 JSON 数组字符串
- 数值比较（GT/LT/GE/LE）会尝试将实际值转为 `double` 比较

---

## 三、接口说明

### 3.1 保存条件配置（草稿版本）

**接口路径**：作为流程定义保存的一部分，由 `ProcessDefinitionServiceImpl.save()` 或前端独立调用。

**请求方式**：`POST`

**Content-Type**：`application/json`

**请求参数**：

```json
{
  "processDefinitionId": 71,
  "version": "DRAFT",
  "gatewayConditions": [
    {
      "sequenceFlowId": "flow_1",
      "sourceNodeId": "gateway_1",
      "targetNodeId": "task_manager",
      "conditionType": "NATIVE",
      "isDefault": false,
      "nativeExpression": "${amount > 10000}"
    },
    {
      "sequenceFlowId": "flow_2",
      "sourceNodeId": "gateway_1",
      "targetNodeId": "task_finance",
      "conditionType": "CUSTOM",
      "isDefault": false,
      "ruleTree": {
        "nodeType": "AND",
        "children": [
          {
            "nodeType": "CONDITION",
            "category": "FORM_FIELD",
            "fieldKey": "amount",
            "operator": "GT",
            "value": "5000"
          },
          {
            "nodeType": "OR",
            "children": [
              {
                "nodeType": "CONDITION",
                "category": "HANDLER_DEPT",
                "fieldKey": "deptId",
                "operator": "IN",
                "value": "[\"1\",\"2\"]"
              },
              {
                "nodeType": "CONDITION",
                "category": "PREV_HANDLER_ROLE",
                "fieldKey": "roleId",
                "operator": "EQ",
                "value": "3"
              }
            ]
          }
        ]
      }
    },
    {
      "sequenceFlowId": "flow_default",
      "sourceNodeId": "gateway_1",
      "targetNodeId": "task_end",
      "isDefault": true
    }
  ]
}
```

**参数说明**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `processDefinitionId` | Integer | **是** | 流程定义ID |
| `version` | String | **是** | 版本号，固定 `"DRAFT"` |
| `gatewayConditions` | List | **是** | 条件配置列表，全量覆盖 |

**后端行为**：
1. 删除该流程定义 DRAFT 版本的所有旧条件配置
2. 逐条插入新的条件配置
3. 对 CUSTOM 模式的规则树，递归保存 `process_gateway_condition_node` 记录

**响应**：`HttpResult<Void>`

---

### 3.2 查询条件配置（按流程定义 + 版本）

**接口路径**：暂无独立接口，通常在 `ProcessDefinitionServiceImpl.info()` 中一并返回。

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `processDefinitionId` | Integer | **是** | 流程定义ID |
| `version` | String | **是** | 版本号 |

**返回示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "gatewayConditions": [
      {
        "id": 101,
        "processDefinitionId": 71,
        "version": "DRAFT",
        "sequenceFlowId": "flow_1",
        "sourceNodeId": "gateway_1",
        "targetNodeId": "task_manager",
        "conditionType": "NATIVE",
        "isDefault": false,
        "nativeExpression": "${amount > 10000}"
      },
      {
        "id": 102,
        "processDefinitionId": 71,
        "version": "DRAFT",
        "sequenceFlowId": "flow_2",
        "sourceNodeId": "gateway_1",
        "targetNodeId": "task_finance",
        "conditionType": "CUSTOM",
        "isDefault": false,
        "ruleTree": {
          "nodeType": "AND",
          "children": [
            {
              "nodeType": "CONDITION",
              "category": "FORM_FIELD",
              "fieldKey": "amount",
              "operator": "GT",
              "value": "5000",
              "sort": 0
            },
            {
              "nodeType": "OR",
              "children": [
                {
                  "nodeType": "CONDITION",
                  "category": "HANDLER_DEPT",
                  "fieldKey": "deptId",
                  "operator": "IN",
                  "value": "[\"1\",\"2\"]",
                  "sort": 0
                },
                {
                  "nodeType": "CONDITION",
                  "category": "PREV_HANDLER_ROLE",
                  "fieldKey": "roleId",
                  "operator": "EQ",
                  "value": "3",
                  "sort": 1
                }
              ],
              "sort": 1
            }
          ],
          "sort": 0
        }
      },
      {
        "id": 103,
        "processDefinitionId": 71,
        "version": "DRAFT",
        "sequenceFlowId": "flow_default",
        "sourceNodeId": "gateway_1",
        "targetNodeId": "task_end",
        "conditionType": null,
        "isDefault": true
      }
    ]
  }
}
```

---

## 四、NATIVE 模式 — JUEL 表达式

### 4.1 表达式语法

NATIVE 模式使用 Flowable 原生的 JUEL（Java Unified Expression Language）表达式。

**基本语法**：
```
${variableName operator value}
```

**示例**：

| 场景 | 表达式 |
|---|---|
| 金额大于 1000 | `${amount > 1000}` |
| 部门等于技术部 | `${deptId == 'tech'}` |
| 金额区间 | `${amount >= 1000 && amount <= 5000}` |
| 类型在白名单 | `${type == 'A' || type == 'B'}` |
| 字符串非空 | `${name != null && name != ''}` |

### 4.2 可用变量

NATIVE 模式下，以下变量在 `PassTaskCommand` 执行前自动注入 Flowable 流程变量：

| 变量名 | 类型 | 说明 |
|---|---|---|
| `formData` | Map<String, Object> | 表单字段值（fieldId → value） |
| `__handler_dept` | List<Integer> | 当前处理人部门ID列表 |
| `__handler_role` | List<Integer> | 当前处理人角色ID列表 |
| `__prev_handler_dept` | List<Integer> | 上一级处理人部门ID列表 |
| `__prev_handler_role` | List<Integer> | 上一级处理人角色ID列表 |

**注意**：
- 表单字段变量名直接使用 `fieldId`（如 `amount`、`leaveType`）
- 内置变量以 `__` 前缀命名
- JUEL 中访问 Map 用 `${formData['amount']}` 或直接 `${amount}`（如果 key 是合法标识符）

### 4.3 表达式与 CUSTOM 模式的对应关系

| CUSTOM 条件 | NATIVE 表达式 |
|---|---|
| `category=FORM_FIELD, fieldKey=amount, operator=GT, value=1000` | `${amount > 1000}` |
| `category=HANDLER_DEPT, operator=IN, value=["1","2"]` | `${__handler_dept.contains('3')}` |
| `category=HANDLER_ROLE, operator=EQ, value="3"` | `${__handler_role.contains('3')}` |



---

## 五、CUSTOM 模式 — 规则树配置

### 5.1 简单单条件

```json
{
  "nodeType": "CONDITION",
  "category": "FORM_FIELD",
  "fieldKey": "amount",
  "operator": "GT",
  "value": "1000"
}
```

含义：`amount > 1000`

### 5.2 AND 组合

```json
{
  "nodeType": "AND",
  "children": [
    {
      "nodeType": "CONDITION",
      "category": "FORM_FIELD",
      "fieldKey": "amount",
      "operator": "GT",
      "value": "1000"
    },
    {
      "nodeType": "CONDITION",
      "category": "FORM_FIELD",
      "fieldKey": "type",
      "operator": "EQ",
      "value": "urgent"
    }
  ]
}
```

含义：`amount > 1000` **AND** `type == 'urgent'`

### 5.3 OR 组合

```json
{
  "nodeType": "OR",
  "children": [
    {
      "nodeType": "CONDITION",
      "category": "HANDLER_DEPT",
      "fieldKey": "deptId",
      "operator": "IN",
      "value": "[\"1\",\"2\"]"
    },
    {
      "nodeType": "CONDITION",
      "category": "HANDLER_ROLE",
      "fieldKey": "roleId",
      "operator": "EQ",
      "value": "3"
    }
  ]
}
```

含义：`部门 IN [1, 2]` **OR** `角色 == 3`

### 5.4 复杂嵌套（AND + OR）

```json
{
  "nodeType": "AND",
  "children": [
    {
      "nodeType": "CONDITION",
      "category": "FORM_FIELD",
      "fieldKey": "amount",
      "operator": "GT",
      "value": "5000"
    },
    {
      "nodeType": "OR",
      "children": [
        {
          "nodeType": "CONDITION",
          "category": "HANDLER_DEPT",
          "fieldKey": "deptId",
          "operator": "IN",
          "value": "[\"1\",\"2\"]"
        },
        {
          "nodeType": "CONDITION",
          "category": "PREV_HANDLER_ROLE",
          "fieldKey": "roleId",
          "operator": "EQ",
          "value": "3"
        }
      ]
    }
  ]
}
```

含义：`amount > 5000` **AND** (`部门 IN [1, 2]` **OR** `上一级角色 == 3`)

---

## 六、前端设计器对接指南

### 6.1 配置流程

```
用户点击连线（sequenceFlow）
  ↓
弹出条件配置面板
  ↓
用户选择模式（NATIVE / CUSTOM / 默认走向）
  ↓
  ├─ NATIVE：输入 JUEL 表达式
  ├─ CUSTOM：拖拽/选择条件组件构建规则树
  └─ 默认走向：无需配置条件
  ↓
保存条件配置到本地状态
  ↓
用户保存整个流程定义时，gatewayConditions 数组随流程定义一起提交
```

### 6.2 提交数据结构

```json
{
  "processDefinition": {
    "id": 71,
    "xmlStr": "...",
    "globalFormBinding": { ... },
    "nodeFormBindings": [ ... ],
    "nodeFieldPermissions": [ ... ]
  },
  "gatewayConditions": [
    {
      "sequenceFlowId": "flow_1",
      "sourceNodeId": "gateway_1",
      "targetNodeId": "task_manager",
      "conditionType": "NATIVE",
      "nativeExpression": "${amount > 10000}"
    },
    {
      "sequenceFlowId": "flow_2",
      "sourceNodeId": "gateway_1",
      "targetNodeId": "task_finance",
      "conditionType": "CUSTOM",
      "ruleTree": { ... }
    }
  ]
}
```

**注意**：`gatewayConditions` 作为流程定义保存/发布的附加参数提交。后端在 `save()` 或独立接口中处理。

### 6.3 规则树可视化建议

```
┌─────────────────────────────────────────────┐
│  条件配置 - flow_2（gateway_1 → task_finance）│
├─────────────────────────────────────────────┤
│  [ AND ]                                    │
│    ├─ 表单字段 [amount] > 5000              │
│    └─ [ OR ]                                │
│         ├─ 当前部门 IN [1, 2]               │
│         └─ 上一级角色 == 3                  │
├─────────────────────────────────────────────┤
│  [+ 添加条件]  [+ 添加 OR 组]               │
└─────────────────────────────────────────────┘
```

---

## 七、默认走向

### 7.1 何时需要默认走向

| 网关类型 | 建议配置默认走向 | 原因 |
|---|---|---|
| **排他网关** | **强烈建议** | 所有条件都不满足时，Flowable 会抛异常 |
| **包容网关** | 建议 | 所有条件都不满足时，行为取决于引擎实现 |
| **并行网关** | 不需要 | 所有分支并行执行，与条件无关 |

### 7.2 默认走向的数据结构

```json
{
  "sequenceFlowId": "flow_default",
  "sourceNodeId": "gateway_1",
  "targetNodeId": "task_end",
  "isDefault": true
}
```

**特点**：
- `isDefault = true`
- 不需要 `conditionType`、`nativeExpression`、`ruleTree`
- 在 BPMN XML 中，会在 `exclusiveGateway` 上设置 `default="flow_default"`

---

## 八、版本管理

### 8.1 生命周期

| 操作 | 条件配置行为 |
|---|---|
| **保存草稿** | 写入 `version = "DRAFT"` |
| **发布** | `copyVersion(id, "DRAFT", newVersion)` |
| **停用** | `copyVersion(id, latestVersion, "DRAFT")` |
| **回滚** | `copyVersion(id, targetVersion, "DRAFT")` |
| **删除** | `deletePhysicsByDefAndVersion(id, null)` |

### 8.2 查询版本

流程运行时，条件配置从 **流程定义的发布版本** 读取。例如：
- 流程定义版本 `"1"` 发布的条件配置
- 后续流程定义升级到 `"2"`
- 但已发起的流程实例（基于 `"1"` 发起）仍然使用 `"1"` 的条件配置

---

## 九、常见问题

### Q1: NATIVE 和 CUSTOM 可以混用吗？

**可以**。同一网关内，部分连线用 NATIVE，部分用 CUSTOM，Flowable 按顺序评估，行为一致。

### Q2: 表单字段变更后，历史条件会失效吗？

**不会**。条件绑定的是 `fieldId`（业务键），只要 `fieldId` 不变，即使字段标题修改，条件仍然有效。

### Q3: 上一节点有多人审批（会签），`PREV_HANDLER_DEPT` 取谁？

**取所有上一节点处理人的部门去重集合**。例如上一节点 3 人审批，分别属于部门 [1]、[2]、[2]，则 `PREV_HANDLER_DEPT` = `[1, 2]`。

### Q4: `IN` 运算符的 value 格式是什么？

必须是 **JSON 数组字符串**：
```json
"[\"value1\",\"value2\"]"
```

### Q5: 默认走向不配置会怎样？

- **排他网关**：所有条件都不满足时，Flowable 抛异常，流程挂起
- **包容网关**：所有条件都不满足时，可能没有分支被激活（取决于 Flowable 实现）

---

## 十、变更记录

| 日期 | 版本 | 说明 |
|---|---|---|
| 2026-05-30 | v1.0 | 初始版本，支持 NATIVE / CUSTOM 双模式，11 种运算符，5 种条件来源 |
