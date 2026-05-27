# 流程定义版本管理 - 前端对接文档

## 接口变更总览

| 接口 | 变更类型 | 说明 |
|---|---|---|
| POST /processDefinition/add | 改动 | 内部逻辑变更，请求参数不变 |
| POST /processDefinition/save | 改动 | 内部逻辑变更，请求参数不变 |
| POST /processDefinition/deploy | 改动 | 内部逻辑变更，请求参数不变 |
| POST /processDefinition/remove | 改动 | 内部逻辑变更，请求参数不变 |
| POST /processDefinition/stop | 改动 | 内部逻辑变更，请求参数不变 |
| POST /processDefinition/info | **改动** | 新增可选参数 `version` |
| POST /processDefinition/versionList | **新增** | 查看历史版本列表 |
| POST /processDefinition/rollback | **新增** | 回滚到指定版本 |

---

## 1. 添加流程定义

**无变更，请求参数不变。**

```
POST /processDefinition/add
```

内部变化：主表 `version` 初始值从 `"0"` 改为 `"DRAFT"`，bytearray 按 DRAFT 版本存储。

---

## 2. 保存流程定义

**无变更，请求参数不变。**

```
POST /processDefinition/save
```

内部变化：保存操作覆盖的是 DRAFT 版本的 bytearray，不再直接覆盖唯一记录。

---

## 3. 发布部署

**无变更，请求参数不变。**

```
POST /processDefinition/deploy?id=1
```

内部变化流程：
1. 读取 DRAFT 版本的 bytearray
2. 验证 BPMN
3. 部署到 Flowable
4. 复制 DRAFT → 新版本号（如 "1"、"2"）
5. 删除 DRAFT
6. 更新主表 version 和 status

---

## 4. 停用

**无变更，请求参数不变。**

```
POST /processDefinition/stop
```

内部变化：停用后自动将最新版本的 bytearray 复制回 DRAFT，方便后续编辑重新发布。

---

## 5. 删除 / 销毁

**无变更，请求参数不变。**

内部变化：会物理删除该流程定义所有版本的 bytearray 数据。

---

## 6. 查看详情（改动）

```
POST /processDefinition/info
```

### 请求参数

```json
{
  "id": 1
}
```

### 新增查询参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| version | String | 否 | 指定查看的版本号 |

### 请求示例

```
POST /processDefinition/info?version=1
```

```json
{
  "id": 1
}
```

### 版本读取规则

| 流程状态 | 不传 version | 传 version |
|---|---|---|
| 草稿（status=0） | 读取 DRAFT 版本 | 读取指定版本 |
| 已发布（status=1） | 读取当前发布版本 | 读取指定版本 |
| 已停用（status=-1） | 读取 DRAFT 版本 | 读取指定版本 |

### 响应

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "processKey": "expense",
    "processName": "报销流程",
    "processCategory": "finance",
    "processDescription": "费用报销审批流程",
    "version": "2",
    "status": "1",
    "createBy": "user001",
    "createByName": "管理员",
    "createTime": "2026-05-01 10:00:00",
    "updateTime": "2026-05-20 14:30:00",
    "xmlStr": "<bpmn:definitions>...</bpmn:definitions>",
    "rawData": { ... },
    "deletable": false,
    "globalFormBinding": {
      "id": 3,
      "processDefinitionId": 1,
      "version": "2",
      "formId": "form_main_001",
      "formVersion": "2",
      "bindType": "GLOBAL",
      "nodeId": null,
      "inheritMainForm": "0"
    },
    "nodeFormBindings": [
      {
        "id": 5,
        "processDefinitionId": 1,
        "version": "2",
        "formId": "form_abc123",
        "formVersion": "3",
        "bindType": "NODE",
        "nodeId": "Activity_1",
        "inheritMainForm": "1"
      }
    ],
    "nodeFieldPermissions": [
      {
        "id": 8,
        "processDefinitionId": 1,
        "version": "2",
        "nodeId": "Activity_1",
        "fieldKey": "amount",
        "permission": "READONLY"
      }
    ]
  }
}
```

**新增字段：**

| 字段 | 类型 | 说明 |
|---|---|---|
| deletable | Boolean | 是否可删除。从未部署过的纯草稿流程为 `true`，其余为 `false` |
| globalFormBinding | Object | 全局表单绑定。未绑定时为 `null` |
| nodeFormBindings | Array | 节点表单绑定列表。无节点绑定时为 `[]` |
| nodeFieldPermissions | Array | 节点字段权限列表。无权限配置时为 `[]` |

---

## 7. 版本列表（新增）

```
POST /processDefinition/versionList
```

### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| processDefinitionId | Integer | 是 | 流程定义ID |

### 请求示例

```
POST /processDefinition/versionList?processDefinitionId=1
```

### 响应

```json
{
  "code": 200,
  "data": [
    {
      "id": 15,
      "processDefinitionId": 1,
      "version": "2",
      "xml": null,
      "rawData": null,
      "createBy": "user001",
      "createTime": "2026-05-20 14:30:00"
    },
    {
      "id": 10,
      "processDefinitionId": 1,
      "version": "1",
      "xml": null,
      "rawData": null,
      "createBy": "user001",
      "createTime": "2026-05-01 10:00:00"
    }
  ]
}
```

> 注意：版本列表中 `xml` 和 `rawData` 字段为 null（不返回大字段数据），查看具体版本内容请调用 `/info?version=1`。

### 前端展示建议

```
┌──────────────────────────────────────────────┐
│  版本历史                                      │
├──────────┬──────────────┬──────────┬─────────┤
│  版本     │  部署时间     │  操作人   │  操作    │
├──────────┼──────────────┼──────────┼─────────┤
│  V2      │ 05-20 14:30  │  管理员   │  查看    │
│  V1      │ 05-01 10:00  │  管理员   │  查看 回滚│
└──────────┴──────────────┴──────────┴─────────┘
```

---

## 8. 回滚到指定版本（新增）

将指定版本的 BPMN 数据复制回 DRAFT，流程状态改为草稿，可编辑后重新发布。

```
POST /processDefinition/rollback
```

### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| processDefinitionId | Integer | 是 | 流程定义ID |
| targetVersion | String | 是 | 目标版本号（如 "1"） |

### 请求示例

```
POST /processDefinition/rollback?processDefinitionId=1&targetVersion=1
```

### 响应

```json
{
  "code": 200,
  "msg": "success"
}
```

### 回滚后的状态

| 属性 | 值 |
|---|---|
| status | "0"（草稿） |
| version | 不变（仍是最后一次发布的版本号） |
| bytearray | DRAFT 版本 = 目标版本的内容副本 |

### 前端交互流程

```
点击"回滚" → 确认弹窗"确认回滚到 V1？回滚后流程变为草稿状态"
  → 确认 → 调用 /rollback
  → 成功 → 刷新页面，流程编辑器加载 DRAFT 内容（即 V1 的副本）
  → 用户可编辑 → 保存 → 发布（生成 V3）
```

> 注意：回滚不会删除任何历史版本，V2 仍然保留在版本列表中。回滚只是把目标版本复制一份到 DRAFT。

---

## 版本生命周期前端交互

```
┌─────────┐     保存      ┌─────────┐     发布      ┌──────────┐
│  新增    │ ──────────▶ │  DRAFT   │ ──────────▶ │ 已发布 V1 │
│         │             │ (可反复覆盖)│             │  status=1 │
└─────────┘             └─────────┘             └─────┬────┘
                                                      │ 停用
                                                      ▼
                                                  ┌──────────┐
                                                  │ 已停用    │
                                                  │ status=-1 │
                                                  │ DRAFT=V1副本│
                                                  └─────┬────┘
                                                      │ 保存+发布
                                                      ▼
                                                  ┌──────────┐
                                                  │ 已发布 V2 │
                                                  └──────────┘

任意已发布版本可回滚：
  V2 → 回滚到 V1 → DRAFT = V1副本 → 编辑 → 发布 V3
```

---

## 前端需要关注的变更点

### 1. 流程列表页（queryPage）

主表 `version` 字段含义变更：
- 旧：`"0"` 表示草稿，`"1"/"2"` 表示 Flowable 版本号
- 新：`"DRAFT"` 表示草稿，`"1"/"2"` 表示发布版本号

**展示建议：**
| version 值 | 显示 |
|---|---|
| DRAFT | 草稿 |
| 1, 2, 3... | V1, V2, V3... |

**新增字段：**

| 字段 | 类型 | 说明 |
|---|---|---|
| deletable | Boolean | `true` 表示可删除（从未部署过的纯草稿），`false` 表示不可删除 |

**删除按钮控制：**

```
if (record.deletable) {
    显示删除按钮
} else {
    隐藏/禁用删除按钮（tooltip："已部署过的流程无法删除"）
}
```

### 2. 流程编辑器（info）

- 编辑器加载时，调用 `/info` 不传 version，自动获取 DRAFT 或当前发布版本
- 查看历史版本时，传 `version` 参数

### 3. 新增版本管理入口

在流程定义详情页或编辑页，建议增加：
- **版本历史**按钮 → 弹窗展示版本列表
- **回滚**按钮 → 确认后调用 `/rollback`
- 版本列表中每行可**查看**（调用 `/info?version=x` 加载到编辑器预览）

### 4. 发布按钮状态

| 流程状态 | DRAFT 数据 | 发布按钮 |
|---|---|---|
| 草稿（status=0） | 有 | 可用 |
| 已发布（status=1） | 无 | 隐藏/禁用 |
| 已停用（status=-1） | 有（上次发布副本） | 可用 |
