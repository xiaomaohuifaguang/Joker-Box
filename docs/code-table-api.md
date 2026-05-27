# 码表管理后端接口文档

## 1. 说明

码表用于维护全局共享的下拉、单选、多选、级联等候选项。动态表单不新增字段类型，也不新增 `optionSource.type`，前端通过现有远程选项配置接入：

```json
{
  "type": "API",
  "url": "/code-table/options",
  "method": "GET",
  "params": {
    "code": "project_type"
  },
  "mapping": {
    "listPath": "data",
    "labelPath": "label",
    "valuePath": "value",
    "childrenPath": "children"
  }
}
```

统一返回结构使用现有 `HttpResult` 包装，业务数据在 `data` 字段中。

## 2. 通用枚举

### 2.1 是否树形 `tree`

| 值 | 含义 |
| --- | --- |
| `0` | 平铺码表 |
| `1` | 树形码表 |

### 2.2 状态 `status`

| 值 | 含义 |
| --- | --- |
| `0` | 停用 |
| `1` | 启用 |

### 2.3 删除标记 `deleted`

| 值 | 含义 |
| --- | --- |
| `0` | 未删除 |
| `1` | 已删除 |

前端一般不需要传 `deleted`。

## 3. 码表对象 `CodeTable`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | string | 更新/删除/详情必填 | 码表 ID |
| `code` | string | 新增/更新必填 | 码表编码，全局唯一。动态表单 `params.code` 使用该值 |
| `name` | string | 新增/更新必填 | 码表名称 |
| `tree` | string | 新增/更新必填 | 是否树形：`0` 否，`1` 是 |
| `status` | string | 新增/更新必填 | 状态：`0` 停用，`1` 启用 |
| `remark` | string | 否 | 备注 |
| `deleted` | string | 否 | 逻辑删除标记，前端不用传 |
| `createBy` | string | 否 | 创建人 |
| `createTime` | string | 否 | 创建时间 |
| `updateTime` | string | 否 | 更新时间 |

### 3.1 `code` 规则

- 必填，长度不超过 64。
- 只允许字母、数字、下划线、中划线、点。
- 必须以字母开头。
- 正则：`^[a-zA-Z][a-zA-Z0-9_.-]{0,63}$`。
- 全局唯一，逻辑删除数据不占用唯一性。

## 4. 码项对象 `CodeItem`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | string | 更新/删除必填 | 码项 ID |
| `tableId` | string | 新增/更新/查询必填 | 所属码表 ID |
| `parentId` | string | 否 | 父级码项 ID，树形码表使用 |
| `label` | string | 新增/更新必填 | 显示文本 |
| `value` | string | 新增/更新必填 | 提交值，同一码表内唯一 |
| `sort` | number | 否 | 排序，未传默认 `0` |
| `status` | string | 新增/更新必填 | 状态：`0` 停用，`1` 启用 |
| `remark` | string | 否 | 备注 |
| `deleted` | string | 否 | 逻辑删除标记，前端不用传 |
| `createBy` | string | 否 | 创建人 |
| `createTime` | string | 否 | 创建时间 |
| `updateTime` | string | 否 | 更新时间 |

### 4.1 码项规则

- `tableId` 必填，且码表必须存在。
- `label` 必填，长度不超过 128。
- `value` 必填，长度不超过 128。
- `value` 在同一个码表内唯一，逻辑删除数据不占用唯一性。
- 平铺码表 `tree = "0"`：`parentId` 必须为空。
- 树形码表 `tree = "1"`：`parentId` 可以为空；不为空时必须属于同一个码表。
- 更新码项时不能把自己设置为自己的父级。
- 更新码项时不能把自己的子孙节点设置为父级。

## 5. 码表管理接口

### 5.1 分页查询码表

```http
POST /code-table/page
Content-Type: application/json
```

请求体：

```json
{
  "current": 1,
  "size": 10,
  "code": "project",
  "name": "项目",
  "tree": "0",
  "status": "1"
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `current` | number | 否 | 当前页，默认 `1` |
| `size` | number | 否 | 每页条数，默认 `10` |
| `code` | string | 否 | 码表编码，模糊查询 |
| `name` | string | 否 | 码表名称，模糊查询 |
| `tree` | string | 否 | 是否树形，精确查询 |
| `status` | string | 否 | 状态，精确查询 |

返回示例：

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": "table1",
        "code": "project_type",
        "name": "项目类型",
        "tree": "0",
        "status": "1",
        "remark": "项目类型选项"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1
  }
}
```

### 5.2 新增码表

```http
POST /code-table/add
Content-Type: application/json
```

请求体：

```json
{
  "code": "project_type",
  "name": "项目类型",
  "tree": "0",
  "status": "1",
  "remark": "项目类型选项"
}
```

返回：`data` 为新增后的 `CodeTable`。

### 5.3 更新码表

```http
POST /code-table/update
Content-Type: application/json
```

请求体：

```json
{
  "id": "table1",
  "code": "project_type",
  "name": "项目类型",
  "tree": "0",
  "status": "1",
  "remark": "项目类型选项"
}
```

返回：`data` 为更新后的 `CodeTable`。

### 5.4 删除码表

```http
POST /code-table/delete?id=table1
```

说明：

- 删除使用逻辑删除。
- 删除码表时，会同步逻辑删除该码表下所有码项。

返回示例：

```json
{
  "code": 200,
  "data": null
}
```

### 5.5 码表详情

```http
POST /code-table/detail?id=table1
```

返回：`data` 为 `CodeTable`。

### 5.6 查询动态表单候选项

```http
GET /code-table/options?code=project_type
```

说明：

- `code` 必填，对应 `CodeTable.code`。
- 码表不存在时返回业务错误：`码表不存在: project_type`。
- 码表停用时返回业务错误：`码表已停用: project_type`。
- 只返回启用码项：`status = "1"`。
- 按 `sort ASC, createTime ASC` 排序。
- 平铺码表返回一维数组。
- 树形码表返回嵌套数组。
- 叶子节点不输出 `children`。

平铺返回示例：

```json
{
  "code": 200,
  "data": [
    {
      "label": "研发项目",
      "value": "dev"
    },
    {
      "label": "实施项目",
      "value": "delivery"
    }
  ]
}
```

树形返回示例：

```json
{
  "code": 200,
  "data": [
    {
      "label": "中国",
      "value": "cn",
      "children": [
        {
          "label": "北京",
          "value": "beijing"
        }
      ]
    }
  ]
}
```

## 6. 码项管理接口

### 6.1 查询码项列表

```http
POST /code-item/list
Content-Type: application/json
```

请求体：

```json
{
  "tableId": "table1",
  "parentId": null,
  "label": "研发",
  "value": "dev",
  "status": "1"
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `tableId` | string | 是 | 码表 ID |
| `parentId` | string | 否 | 父级码项 ID，精确查询。传 `null` 查询根节点/平铺项 |
| `label` | string | 否 | 显示文本，模糊查询 |
| `value` | string | 否 | 提交值，模糊查询 |
| `status` | string | 否 | 状态，精确查询 |

返回：`data` 为 `CodeItem[]`。

### 6.2 新增码项

```http
POST /code-item/add
Content-Type: application/json
```

平铺码表请求体：

```json
{
  "tableId": "table1",
  "label": "研发项目",
  "value": "dev",
  "sort": 1,
  "status": "1",
  "remark": ""
}
```

树形码表子节点请求体：

```json
{
  "tableId": "areaTableId",
  "parentId": "parentItemId",
  "label": "北京",
  "value": "beijing",
  "sort": 1,
  "status": "1"
}
```

返回：`data` 为新增后的 `CodeItem`。

### 6.3 更新码项

```http
POST /code-item/update
Content-Type: application/json
```

请求体：

```json
{
  "id": "item1",
  "tableId": "table1",
  "parentId": null,
  "label": "研发项目",
  "value": "dev",
  "sort": 1,
  "status": "1",
  "remark": ""
}
```

返回：`data` 为更新后的 `CodeItem`。

### 6.4 删除码项

```http
POST /code-item/delete?id=item1
```

说明：

- 删除使用逻辑删除。
- 平铺码表：只删除当前码项。
- 树形码表：删除当前码项及所有子孙码项。

返回示例：

```json
{
  "code": 200,
  "data": null
}
```

### 6.5 查询码项树

```http
POST /code-item/tree
Content-Type: application/json
```

请求体：

```json
{
  "tableId": "areaTableId",
  "status": "1"
}
```

返回：`data` 为 `CodeOption[]`，结构与 `/code-table/options` 的树形返回一致。

```json
{
  "code": 200,
  "data": [
    {
      "label": "中国",
      "value": "cn",
      "children": [
        {
          "label": "北京",
          "value": "beijing"
        }
      ]
    }
  ]
}
```

## 7. 动态表单接入示例

### 7.1 SELECT / RADIO / CHECKBOX / MULTISELECT

```json
{
  "type": "SELECT",
  "title": "项目类型",
  "optionSource": {
    "type": "API",
    "url": "/code-table/options",
    "method": "GET",
    "params": {
      "code": "project_type"
    },
    "mapping": {
      "listPath": "data",
      "labelPath": "label",
      "valuePath": "value"
    }
  },
  "options": []
}
```

### 7.2 CASCADER / MULTICASCADER

```json
{
  "type": "CASCADER",
  "title": "地区",
  "optionSource": {
    "type": "API",
    "url": "/code-table/options",
    "method": "GET",
    "params": {
      "code": "area"
    },
    "mapping": {
      "listPath": "data",
      "labelPath": "label",
      "valuePath": "value",
      "childrenPath": "children"
    }
  },
  "options": []
}
```

## 8. 前端注意事项

- 管理端新增平铺码表时，码项 `parentId` 不要传值。
- 管理端新增树形码表根节点时，`parentId` 不传或传 `null`。
- 码表停用后，`/code-table/options` 会返回业务错误，不返回候选项。
- 码项停用后，不会出现在 `/code-table/options` 结果中。
- 动态表单发布时，后端不校验 `params.code` 对应码表是否存在。
- 动态表单提交时，后端不校验提交值是否仍存在于码表。
- 远程 options 加载结果应作为运行时基础选项缓存，不要回写到字段配置 `options`。
