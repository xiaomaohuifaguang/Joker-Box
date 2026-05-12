# 动态表单 - 接口文档

> 作者：小猫会发光
> 日期：2026-05-09

## 一、实体关系

```
DynamicForm（表单定义）
  ├── 1 对 N：DynamicFormFieldGroup（字段分组）── 版本隔离
  │       └── 1 对 N：DynamicFormField（表单项）
  ├── 1 对 N：DynamicFormLinkageRule（联动规则）── 版本隔离
  │       └── 1 对 N：DynamicFormLinkageNode（条件节点，树形结构）
  └── 1 对 N：DynamicFormInstance（表单数据实例）
          └── 1 对 N：DynamicFormFieldInstance（字段值）
```

**版本管理：** 表单每 `deploy` 一次，版本号 +1。分组、字段、联动规则都跟着版本走。草稿状态版本号为 `"DRAFT"`。

---

## 二、实体字段详解

### 2.1 DynamicForm（表单定义）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 表单ID，UUID |
| name | String | 表单名称 |
| description | String | 描述 |
| version | String | 当前版本号。`DRAFT` 表示草稿，`1`/`2`/`3`... 表示已发布版本 |
| status | String | `0` 草稿 / `1` 已发布 / `-1` 已停用 |
| groups | List | **字段分组列表**（非DB字段，详情时返回） |
| formFields | List | **平铺字段列表**（非DB字段，无分组时返回） |
| linkageRules | List | 联动规则列表（非DB字段） |

### 2.2 DynamicFormFieldGroup（字段分组）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 分组ID，UUID |
| name | String | 分组名称，如"基本信息" |
| description | String | 分组描述 |
| sort | Integer | 分组排序，越小越靠前 |
| collapsed | String | `0` 展开 / `1` 折叠 |
| fields | List | 分组下的字段列表（非DB字段） |

### 2.3 DynamicFormField（表单项）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | String | - | 字段ID，UUID，后端生成 |
| fieldId | String | 是 | **前端设计ID**，表单内唯一，如 `field_abc123`，用于 data 的 key |
| groupId | String | 否 | 所属分组ID |
| title | String | 是 | 字段标题，如"姓名" |
| type | Enum | 是 | 字段类型，见下方枚举 |
| required | String | 否 | `1` 必填，`0` 选填 |
| defaultValue | String | 否 | 默认值 |
| placeholder | String | 否 | 输入提示 |
| options | List | 否 | 单选/多选/下拉选项 |
| minLength | Integer | 否 | 最小长度 |
| maxLength | Integer | 否 | 最大长度 |
| min | Integer | 否 | 最小值（NUMBER类型） |
| max | Integer | 否 | 最大值（NUMBER类型） |
| pattern | String | 否 | 正则表达式 |
| patternTips | String | 否 | 正则校验失败提示 |
| span | Integer | 否 | 宽度 1-24，默认24占满一行 |
| sort | Integer | 否 | 组内排序 |

**字段类型枚举：**

| 类型 | 说明 | 需 options |
|------|------|-----------|
| INPUT | 输入框 | 否 |
| NUMBER | 数字 | 否 |
| SELECT | 下拉选择 | 是 |
| MULTISELECT | 多选下拉 | 是 |
| RADIO | 单选 | 是 |
| CHECKBOX | 多选 | 是 |
| DATE | 日期 | 否 |
| DATETIME | 日期时间 | 否 |
| TIME | 时间 | 否 |
| SWITCH | 开关 | 否 |
| TEXTAREA | 多行文本 | 否 |
| UPLOAD | 上传 | 否 |
| RATE | 评分 | 否 |
| SLIDER | 滑块 | 否 |
| COLOR | 颜色选择 | 否 |
| CASCADER | 级联选择 | 是（支持children嵌套） |
| MULTICASCADER | 多选级联 | 是 |

### 2.4 DynamicFormOption（选项配置）

| 字段 | 类型 | 说明 |
|------|------|------|
| label | String | 显示文本 |
| value | String | 选项值 |
| children | List | 子选项（级联时使用） |

### 2.5 DynamicFormLinkageRule（联动规则）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 规则ID |
| name | String | 规则名称 |
| targetFieldId | String | 目标字段 fieldId |
| actionType | String | 动作类型 |
| actionValue | Object | 动作参数 |
| enable | Boolean | 是否启用 |
| conditionTree | List | 条件节点树 |

**actionType 枚举：**

| 类型 | 说明 | actionValue 示例 |
|------|------|------------------|
| SHOW | 显示字段 | `true` |
| HIDE | 隐藏字段 | `true` |
| REQUIRED | 设为必填 | `true` / `false` |
| OPTION | 设置选项 | `[{"label":"A","value":"1"}]` |
| VALUE | 设置值 | `"自动填充的值"` |
| DISABLED | 禁用字段 | `true` |
| ENABLED | 启用字段 | `true` |
| SET_PATTERN | 设置正则 | `"^\\d+$"` |
| SET_SPAN | 设置宽度 | `12` |

### 2.6 DynamicFormLinkageNode（联动条件节点）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 节点ID |
| nodeType | String | `AND` / `OR` / `CONDITION` |
| triggerFieldId | String | 触发字段 fieldId（CONDITION时用） |
| triggerCondition | String | 触发条件（CONDITION时用） |
| triggerValue | Object | 触发值（CONDITION时用） |
| children | List | 子节点（AND/OR时用） |

**triggerCondition 枚举：** EQ / NE / GT / LT / GE / LE / IN / NOT_IN / EMPTY / NOT_EMPTY / REGEX

### 2.7 FormData（表单数据提交）

| 字段 | 类型 | 说明 |
|------|------|------|
| formId | String | 表单ID |
| version | String | 版本号，不传使用最新版本 |
| formInstanceId | String | 实例ID，传则更新，不传则新建 |
| data | Map | 表单数据，**key 为 fieldId** |

---

## 三、版本流转

```
草稿（DRAFT）
    ↓ deploy
已发布（1） → deploy → 已发布（2） → deploy → 已发布（3）
    ↓ stop（基于最新版本回退草稿）
草稿（DRAFT，基于版本3）
```

- **草稿（0）**：可随意增删改字段、分组、联动规则
- **发布（1）**：`deploy` 时将 DRAFT 复制为新版本，清空 DRAFT
- **停用（-1）**：`stop` 时将最新版本复制为 DRAFT，可继续编辑

---

## 四、前后端数据约定

### 4.1 有分组的表单（推荐）

前端设计器保存时传 `groups`，字段放在分组内：

```json
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
      "actionValue": true,
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

### 4.2 无分组的表单（兼容存量）

不传 `groups`，传 `formFields` 平铺列表：

```json
{
  "name": "简单表单",
  "formFields": [
    { "fieldId": "field_1", "title": "姓名", "type": "INPUT" }
  ]
}
```

**后端处理：**
- 有 `groups` → 按分组结构保存，字段写入对应 `groupId`
- 无 `groups` 有 `formFields` → 平铺保存，`groupId` 为 null

### 4.3 详情返回结构

有分组时返回 `groups`，无分组时返回 `formFields`：

```json
{
  "code": 200,
  "data": {
    "id": "form_xxx",
    "name": "请假申请单",
    "version": "3",
    "status": "1",
    "groups": [
      {
        "id": "g_xxx",
        "name": "基本信息",
        "description": "请填写申请人基本信息",
        "sort": 1,
        "collapsed": "0",
        "fields": [
          {
            "id": "f_xxx",
            "fieldId": "field_name",
            "groupId": "g_xxx",
            "title": "姓名",
            "type": "INPUT",
            "required": "1",
            "span": 12,
            "sort": 1
          }
        ]
      }
    ],
    "linkageRules": [...]
  }
}
```

**前端判断方式：**
```js
if (res.data.groups && res.data.groups.length > 0) {
  // 按分组渲染折叠面板 / 卡片
} else if (res.data.formFields) {
  // 按平铺列表渲染
}
```

---

## 五、API 接口清单

| 接口 | 路径 | 说明 |
|------|------|------|
| 添加 | `POST /dynamicForm/add` | 创建表单及 DRAFT 版本 |
| 修改 | `POST /dynamicForm/update` | 修改草稿表单（覆盖 DRAFT） |
| 删除 | `POST /dynamicForm/remove` | 删除草稿表单（仅 status=0） |
| 详情 | `POST /dynamicForm/info` | 获取表单定义（含字段/分组/联动） |
| 分页 | `POST /dynamicForm/queryPage` | 表单列表 |
| 发布 | `POST /dynamicForm/deploy` | 草稿 → 新版本 |
| 停用 | `POST /dynamicForm/stop` | 退回草稿（基于最新版本） |
| 提交数据 | `POST /dynamicForm/submit` | 用户填写并提交表单数据 |

---

## 六、接口详情

### 6.1 添加表单

**POST** `/dynamicForm/add`

**请求体：** `DynamicForm`

- 支持传 `groups`（分组结构）或 `formFields`（平铺列表）
- 传 `groups` 时，字段放在各分组的 `fields` 内
- 全局校验：所有字段的 `fieldId` 不可重复

**响应：** `HttpResult<Void>`

---

### 6.2 修改表单

**POST** `/dynamicForm/update`

**请求体：** `DynamicForm`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | String | 是 | 表单ID |
| name | String | 否 | 新名称 |
| description | String | 否 | 新描述 |
| groups | List | 否 | 新分组（覆盖旧 DRAFT） |
| formFields | List | 否 | 新字段（覆盖旧 DRAFT，无分组时用） |
| linkageRules | List | 否 | 新联动规则（覆盖旧 DRAFT） |

> 只有草稿（status=0）和已停用（status=-1）可修改

**响应：** `HttpResult<Void>`

---

### 6.3 删除表单

**POST** `/dynamicForm/remove`

**请求体：**

```json
{ "id": "表单ID" }
```

> 只有草稿（status=0）可删除，且只能删除自己创建的

**响应：** `HttpResult<Void>`

---

### 6.4 表单详情

**POST** `/dynamicForm/info`

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | String | 是 | 表单ID |
| version | String | 否 | 版本号，不传返回最新版本 |

**响应：** `HttpResult<DynamicForm>`

- 有分组时返回 `groups`，`formFields` 为 null
- 无分组时返回 `formFields`，`groups` 为 null

---

### 6.5 分页查询

**POST** `/dynamicForm/queryPage`

**请求体：** `PageParam`

| 字段 | 类型 | 说明 |
|------|------|------|
| current | long | 页码，默认1 |
| size | long | 页大小，默认10 |

**响应：** `HttpResult<Page<DynamicForm>>`

> 返回的表单列表不含字段详情，只有基础信息

---

### 6.6 发布表单

**POST** `/dynamicForm/deploy?formId={formId}`

> 将 DRAFT 版本复制为新版本号，清空 DRAFT，状态变为已发布

**响应：** `HttpResult<Void>`

---

### 6.7 停用表单

**POST** `/dynamicForm/stop?formId={formId}`

> 将最新版本复制为 DRAFT，状态变为已停用，可继续编辑后重新发布

**响应：** `HttpResult<Void>`

---

### 6.8 提交表单数据

**POST** `/dynamicForm/submit`

**请求体：** `FormData`

```json
{
  "formId": "表单ID",
  "version": "2",
  "formInstanceId": "实例ID（更新时传）",
  "data": {
    "field_name": "张三",
    "field_dept": "tech",
    "field_type": "sick",
    "field_days": 2
  }
}
```

**data 说明：**
- key 是 `fieldId`（不是数据库 id）
- value 类型根据字段类型变化：INPUT/NUMBER 传字符串/数字，SELECT/RADIO 传选项 value，CHECKBOX/MULTISELECT 传 value 数组

**响应：** `HttpResult<Void>`

**校验流程：**
1. 校验表单存在且已发布
2. 加载对应版本的字段和联动规则
3. 计算联动效果（HIDE/DISABLED 的字段跳过必填校验）
4. 字段级校验：必填、长度、数值范围、正则

---

## 七、异常情况汇总

| 触发条件 | 异常/说明 |
|----------|-----------|
| fieldId 重复 | `IllegalArgumentException("表单项 fieldId 存在重复")` |
| 表单项校验失败 | `IllegalArgumentException("表单项校验失败: {title}")` |
| 操作非草稿表单 | `return false`（删除/修改时） |
| 操作非本人表单 | `IllegalStateException("无权操作他人表单")` |
| 表单不存在 | `IllegalArgumentException("表单不存在: {id}")` |
| 表单未发布 | `IllegalStateException("表单未发布, 无法提交: {id}")` |
| 必填字段为空 | `IllegalArgumentException("{title} 必填")` |
| 长度/数值/正则不满足 | 对应 `IllegalArgumentException` |
| 未登录 | `IllegalStateException("当前未登录")` |
