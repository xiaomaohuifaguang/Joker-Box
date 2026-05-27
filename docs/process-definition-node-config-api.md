# 流程定义绑定表单 - 前端对接文档

> 前置阅读：[流程定义版本管理](process-definition-version-api.md)（含 `/save`、`/info` 基础说明）
>
> 相关文档：[动态表单 API](dynamic-form-api.md)（含 `/dynamicForm/publishedForms` 接口说明）

---

## 一、设计目标

**全局统一，节点差异化。**

一个流程定义中，大部分节点的表单需求是一致的（比如请假流程所有节点都围绕"请假单"），少数节点需要替换或扩展（比如审批节点增加"审批意见"字段）。

因此设计了两层绑定：

| 层级 | 作用范围 | 使用场景 |
|---|---|---|
| 全局表单绑定 | 所有节点默认继承 | 80% 的流程只需配这个 |
| 节点表单绑定 | 单个节点覆盖/扩展 | 特定节点需要不同表单 |

---

## 二、核心概念

### 2.1 全局表单绑定（globalFormBinding）

流程级别的默认表单。所有未单独配置节点的表单都继承这份表单模板。

```json
{ "formId": "form_main_001", "formVersion": "2" }
```

- `formId`：表单唯一标识
- `formVersion`：锁定的表单版本号，防止表单发布新版本后流程自动变更

### 2.2 节点表单绑定（nodeFormBindings）

覆盖指定节点的表单。当某个节点需要与全局不同的表单时使用。

```json
{
  "formId": "form_abc123",
  "formVersion": "3",
  "nodeId": "Activity_1",
  "inheritMainForm": "1"
}
```

| 字段 | 说明 |
|---|---|
| `nodeId` | BPMN 节点 ID，对应画布上的具体节点 |
| `inheritMainForm` | 是否继承全局表单字段（见 2.4） |

### 2.3 节点字段权限（nodeFieldPermissions）

控制节点上每个字段的可见性/可编辑性。

```json
{ "nodeId": "Activity_1", "fieldKey": "amount", "permission": "READONLY" }
```

| 权限值 | 含义 | 前端行为 |
|---|---|---|
| `VISIBLE` | 可见 | 继承表单模板默认配置 |
| `READONLY` | 只读 | 展示但不可编辑 |
| `HIDDEN` | 隐藏 | 不渲染该字段 |
| `EDITABLE` | 可编辑 | 显式设置为可编辑 |
| `REQUIRED` | 必填 | 强制必填 |

**权限优先级**：节点字段权限 > 表单模板联动规则 > 表单模板默认配置

### 2.4 字段继承（inheritMainForm）

当 `inheritMainForm="1"` 时，节点表单在渲染时会**合并全局表单字段和节点表单字段**，按 `fieldKey` 去重（同名取节点表单的定义）。

典型场景：全局表单有 10 个字段，审批节点只需要额外加 1 个"审批意见"字段，不需要重新定义全部 10 个字段。

```javascript
// 字段合并逻辑
const mainFields = await dynamicFormInfo(globalFormId);
const nodeFields = await dynamicFormInfo(nodeFormId);

const allFields = [...mainFields, ...nodeFields]
  .filter((f, i, arr) => arr.findIndex(t => t.fieldKey === f.fieldKey) === i);
```

---

## 三、数据模型

### 3.1 流程定义表单绑定表（cat_process_definition_form）

| 字段 | 说明 |
|---|---|
| `id` | 主键 |
| `process_definition_id` | 流程定义 ID |
| `version` | 流程定义版本号（DRAFT 或数字版本） |
| `form_id` | 表单 ID |
| `form_version` | 表单版本号 |
| `bind_type` | `GLOBAL` / `NODE` |
| `node_id` | 节点 ID（bind_type=NODE 时有值） |
| `inherit_main_form` | 是否继承全局表单（0/1） |

### 3.2 流程节点字段权限表（cat_process_node_field_permission）

| 字段 | 说明 |
|---|---|
| `id` | 主键 |
| `process_definition_id` | 流程定义 ID |
| `version` | 流程定义版本号 |
| `node_id` | 节点 ID |
| `field_key` | 字段标识 |
| `permission` | 权限值 |

---

## 四、接口说明

### 4.1 保存流程定义（含节点配置）

**POST** `/processDefinition/save`

节点配置跟随流程定义一并保存，**不单独提供保存接口**。

```json
{
  "id": 1,
  "xmlStr": "...",
  "globalFormBinding": { "formId": "form_main_001", "formVersion": "2" },
  "nodeFormBindings": [
    {
      "formId": "form_abc123",
      "formVersion": "3",
      "nodeId": "Activity_1",
      "inheritMainForm": "1"
    }
  ],
  "nodeFieldPermissions": [
    { "nodeId": "Activity_1", "fieldKey": "amount", "permission": "READONLY" },
    { "nodeId": "Activity_1", "fieldKey": "reason", "permission": "REQUIRED" }
  ]
}
```

**保存语义**：

| 字段 | 行为 |
|---|---|
| `globalFormBinding` | 每次保存**先清空再插入**，传 `null` 或 `{}` 则清空 |
| `nodeFormBindings` | 每次保存**先清空再插入**，传 `null` 或 `[]` 则清空 |
| `nodeFieldPermissions` | 每次保存**先清空再插入**，传 `null` 或 `[]` 则清空 |

### 4.2 查询流程定义详情（含节点配置）

**POST** `/processDefinition/info`

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "xmlStr": "...",
    "globalFormBinding": { "id": 3, "formId": "form_main_001", "formVersion": "2", "bindType": "GLOBAL" },
    "nodeFormBindings": [
      { "id": 5, "formId": "form_abc123", "formVersion": "3", "bindType": "NODE", "nodeId": "Activity_1", "inheritMainForm": "1" }
    ],
    "nodeFieldPermissions": [
      { "id": 8, "nodeId": "Activity_1", "fieldKey": "amount", "permission": "READONLY" }
    ],
    "deletable": false
  }
}
```

| 字段 | 空值表现 |
|---|---|
| `globalFormBinding` | 未绑定时为 `null` |
| `nodeFormBindings` | 无节点绑定时为 `[]` |
| `nodeFieldPermissions` | 无权限配置时为 `[]` |

### 4.3 查询已发布表单列表（供绑定下拉框使用）

**POST** `/dynamicForm/publishedForms`

```json
{
  "code": 200,
  "data": [
    {
      "formId": "form_main_001",
      "formName": "请假申请表单",
      "latestVersion": "3",
      "versions": [
        { "version": "1", "publishTime": "2026-05-20 10:00:00" },
        { "version": "2", "publishTime": "2026-05-22 14:30:00" },
        { "version": "3", "publishTime": "2026-05-25 09:00:00" }
      ]
    }
  ]
}
```

---

## 五、前端交互流程

### 5.1 设计器首次加载

```
调用 /info
  ├── 返回 BPMN + rawData → 渲染画布
  ├── 返回 globalFormBinding → 顶部工具栏展示全局表单
  ├── 返回 nodeFormBindings + nodeFieldPermissions → 缓存所有节点配置
  └── 返回 deletable → 控制删除按钮显隐
```

### 5.2 点击某个节点

```
点击节点 "Activity_1"
  ├── 1. 从 /info 缓存中读取该节点的绑定和权限配置
  ├── 2. POST /dynamicForm/info { "id": "formId" } → 获取表单所有字段
  │      与权限列表 merge：有配置 → 使用配置值；无 → 默认 VISIBLE
  └── 3. 渲染节点配置面板
```

### 5.3 表单下拉框交互

```
打开表单选择下拉框
  ├── 1. 调用 POST /dynamicForm/publishedForms
  ├── 2. 下拉选项：formName（展示） + formId（值）
  ├── 3. 选中表单后，默认带入 latestVersion
  └── 4. 提供"切换历史版本"入口（从 versions 列表中选择）
```

**表单下拉框规则**：

| 问题 | 结论 |
|---|---|
| 列表过滤 | 只展示 `status='1'`（已发布）的表单 |
| 版本选择 | 选项同时返回 `id` 和 `version`，用户只选表单，版本号自动带入 |
| 后续升级 | 表单发布新版本后，已绑定的流程**不会自动升级**，需手动重新绑定 |

### 5.4 保存流程

```
点击保存
  ├── 1. 收集画布数据 → xmlStr
  ├── 2. 收集全局表单绑定 → globalFormBinding
  ├── 3. 收集所有节点配置 → nodeFormBindings + nodeFieldPermissions
  └── 4. POST /processDefinition/save（全量提交）
```

### 5.5 发布流程

```
点击发布
  ├── 1. 先保存（同上）
  ├── 2. 调用 POST /processDefinition/deploy
  └── 3. 发布后流程版本从 DRAFT 变为数字版本，配置随版本复制
```

---

## 六、注意事项

1. **编辑限制**：只有 `status=0`（草稿）和 `status=-1`（已停用）的流程允许保存。
2. **版本一致性**：保存节点配置只修改 DRAFT 版本，发布时随 bytearray 一起复制到新版本号。
3. **全量覆盖**：`nodeFormBindings` 和 `nodeFieldPermissions` 在 `/save` 中均为全量覆盖，前端须提交当前所有节点的完整配置。
4. **未配置字段**：没有在 `nodeFieldPermissions` 中出现的字段，默认使用表单模板自身的配置。
5. **startEvent 节点**：支持配置表单绑定和字段权限，逻辑与普通节点一致。