# 流程实例绑定表单 — 设计文档

> 日期：2026-05-27
> 前置：[流程定义绑定表单](process-definition-node-config-api.md)、[动态表单 API](dynamic-form-api.md)

---

## 一、设计目标

将流程定义阶段的表单绑定配置落地到流程实例运行时，实现：

- 发起流程时填写主表单
- 任务流转时填写/查看节点表单
- 字段权限在运行时强制校验
- 草稿保存表单数据

---

## 二、核心决策

| 决策项 | 结论 |
|---|---|
| 数据模式 | 混合模式：主表单 + 节点表单独立存储 |
| 关联方式 | 独立关联表 `cat_process_instance_form` |
| 区分主/节点 | `nodeId`（null = 主表单） |
| 主表单创建 | `start()` 时创建实例 + 初始化所有字段实例 + 填入数据 |
| 节点表单创建 | 到达该节点时按需创建 |
| 节点表单创建位置 | `ApprovalTaskCreateListener` |
| 数据提交 | `/pass` 原子操作（权限校验 + 存储 + 流转） |
| 字段实例创建 | 创建表单实例时一次性初始化所有字段实例（值为 null） |
| 草稿表单 | `saveDraft()` 同时保存主表单数据 |
| 表单渲染数据 | `/info` 一次返回完整表单配置（字段定义 + 权限 + 已填数据） |
| 权限判断 | 后端根据当前登录人是否为任务处理人决定权限模式 |

---

## 三、数据模型

### 3.1 流程实例表单关联表（新建）

```sql
CREATE TABLE cat_process_instance_form (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    process_instance_id INT          NOT NULL COMMENT '流程实例ID',
    node_id      VARCHAR(64)         NULL     COMMENT '节点ID，null=主表单',
    form_id      VARCHAR(64)         NOT NULL COMMENT '表单ID',
    form_version VARCHAR(16)         NOT NULL COMMENT '表单版本（启动时快照）',
    form_instance_id VARCHAR(64)     NOT NULL COMMENT '表单实例ID',
    create_by    VARCHAR(64)         NULL     COMMENT '创建人',
    create_time  DATETIME            NULL     COMMENT '创建时间',
    UNIQUE KEY uk_instance_node (process_instance_id, node_id)
) COMMENT '流程实例表单关联';
```

### 3.2 与现有表的关系

```
cat_process_instance
    │
    ├── 1:N ── cat_process_instance_form（主表单 nodeId=null，节点表单 nodeId=具体节点）
    │              │
    │              └── N:1 ── cat_dynamic_form_instance（form_instance_id）
    │                              │
    │                              └── 1:N ── cat_dynamic_form_field_instance（字段值）
    │
    └── N:1 ── cat_process_definition（定义阶段的绑定配置，运行时读取）
```

---

## 四、表单配置解析（resolveFormConfig）

所有需要表单的场景（草稿、发起、流转、查看）都通过统一的配置解析逻辑确定当前节点的表单来源：

```
输入：processDefinitionId + nodeId（startEvent 传其 nodeId）

第一步：解析全局表单（base）
  ├── 查流程定义的 globalFormBinding
  │      ├── 有 → base = globalFormBinding
  │      └── 无 → base = null

第二步：解析节点配置
  ├── 查流程定义的 nodeFormBindings，找 nodeId 匹配的绑定
  │      ├── 无节点绑定 → 最终表单 = base（无节点权限）
  │      └── 有节点绑定：
  │             ├── inheritMainForm = "1" → 最终表单 = base + 节点表单（合并渲染，base 在前）
  │             └── inheritMainForm = "0" → 最终表单 = 节点表单（忽略 base）

第三步：权限
  └── 有节点绑定时使用该节点的 nodeFieldPermissions，否则使用表单模板默认配置

结果：base + node 都无 → 无表单
```

**渲染顺序**：全局表单字段在前，节点表单字段在后。`inheritMainForm=1` 时按此顺序合并展示，fieldKey 相同取节点表单定义覆盖。

**字段值覆盖优先级**：节点表单定义 > 全局表单定义。

**startEvent 也是节点**，发起流程和保存草稿时传入 startEvent 的 nodeId，走同样的解析逻辑。

---

## 五、运行时数据流

### 5.1 保存草稿

```
POST /processInstance/saveDraft
  ├── 传入：processDefinitionId, title, formData（表单数据）
  ├── 1. 解析表单配置（resolveFormConfig）：
  │      ├── 查 startEvent 节点是否有绑定（nodeFormBindings）
  │      ├── 有 → 使用 startEvent 节点的表单 + 该节点的 nodeFieldPermissions
  │      └── 无 → 使用 globalFormBinding + 无节点权限
  ├── 2. 无任何绑定 → 跳过表单处理
  ├── 3. 查关联表是否已有该节点的表单实例
  │      ├── 无 → 创建 DynamicFormInstance + 初始化字段实例 → 写入关联表
  │      └── 有 → 复用已有实例
  ├── 4. 校验并写入表单数据（不校验必填，草稿允许部分填写）
  │      └── 仍需遵守 READONLY/HIDDEN 字段忽略传值规则
  └── 5. 保存流程实例
```

### 5.2 发起流程

```
POST /processInstance/start
  ├── 传入：processDefinitionId, title, formData（表单数据）
  ├── 1. 解析表单配置（resolveFormConfig）：
  │      ├── 查 startEvent 节点是否有绑定（nodeFormBindings）
  │      ├── 有 → 使用 startEvent 节点的表单 + 该节点的 nodeFieldPermissions
  │      └── 无 → 使用 globalFormBinding + 无节点权限
  ├── 2. 无任何绑定 → 跳过表单处理，直接启动
  ├── 3. 有绑定 → 创建表单实例 + 初始化字段实例
  │      + 按 nodeFieldPermissions 校验字段权限 + 写入数据 + 写入关联表
  ├── 4. 启动 Flowable 流程实例
  ├── 5. 保存 ProcessInstance 记录
  └── 6. 记录处理信息
```

### 5.3 节点表单创建（任务到达时）

```
ApprovalTaskCreateListener.notify()
  ├── 1. 获取当前节点的 nodeId
  ├── 2. 查流程定义的 nodeFormBindings，找到该节点的绑定配置
  │      ├── 无绑定 → 结束
  │      └── 有绑定 → 继续
  ├── 3. 查关联表是否已有该节点的表单实例
  │      ├── 无 → 创建 DynamicFormInstance + 初始化字段实例 + 写入关联表
  │      └── 有 → 复用（驳回场景）
  └── 4. 结束
```

### 5.4 任务流转

```
POST /processInstance/pass
  ├── 传入：processInstanceId, taskId, remark, formData（节点表单数据）
  ├── 1. 查当前任务的 nodeId
  ├── 2. 查关联表获取该节点的表单实例
  │      ├── 无关联 → 无表单，直接流转
  │      └── 有关联 → 继续
  ├── 3. 查流程定义的 nodeFieldPermissions，获取当前节点的字段权限
  ├── 4. 校验表单数据：
  │      ├── REQUIRED 字段必须有值
  │      ├── 只接受 EDITABLE/REQUIRED 字段的值
  │      ├── READONLY/HIDDEN 字段忽略前端传值，保持原值
  │      └── 未在 nodeFieldPermissions 中出现的字段 → 使用表单模板默认配置
  ├── 5. 写入校验后的表单数据
  └── 6. 完成 Flowable 任务
```

### 5.5 查看流程实例详情

```
POST /processInstance/info?id=X&taskId=Y
  ├── 1. 返回原有信息（timeline、handleInfo 等）
  └── 2. 如果传了 taskId：
         ├── 判断当前登录人是否为任务处理人
         ├── 查当前任务的 nodeId
         ├── 查关联表获取表单实例
         ├── 查流程定义的字段权限配置
         └── 组装表单渲染数据返回
```

---

## 六、表单渲染数据结构

`/info` 响应新增 `taskForm` 字段：

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "title": "张三的请假申请",
    "processStatus": "1",
    "timeline": [...],
    "taskForm": {
      "editable": true,
      "formId": "form_main_001",
      "formVersion": "2",
      "formInstanceId": "inst_abc123",
      "formFields": [
        {
          "fieldKey": "applicant",
          "label": "申请人",
          "type": "INPUT",
          "permission": "READONLY",
          "value": "张三",
          "required": false,
          "options": null
        }
      ],
      "groups": [
        {
          "groupId": "grp_001",
          "name": "请假信息",
          "description": null,
          "sort": 1,
          "collapsed": "0",
          "fields": [
            {
              "fieldKey": "reason",
              "label": "请假原因",
              "type": "TEXTAREA",
              "permission": "REQUIRED",
              "value": null,
              "required": true,
              "options": null
            },
            {
              "fieldKey": "days",
              "label": "请假天数",
              "type": "NUMBER",
              "permission": "EDITABLE",
              "value": null,
              "required": false,
              "options": null
            }
          ]
        }
      ],
      "inherited": {
        "formId": "form_main_001",
        "formVersion": "2",
        "formInstanceId": "inst_base456",
        "formFields": [
          {
            "fieldKey": "department",
            "label": "部门",
            "type": "SELECT",
            "permission": "READONLY",
            "value": "技术部",
            "required": false,
            "options": null,
            "sourceFormId": "form_main_001"
          }
        ],
        "groups": [
          {
            "groupId": "grp_base_001",
            "name": "基本信息",
            "description": null,
            "sort": 1,
            "collapsed": "0",
            "fields": [
              {
                "fieldKey": "applicant",
                "label": "申请人",
                "type": "INPUT",
                "permission": "READONLY",
                "value": "张三",
                "required": false,
                "options": null,
                "sourceFormId": "form_main_001"
              }
            ]
          }
        ]
      }
    }
  }
}
```

### taskForm 字段说明

| 字段 | 说明 |
|---|---|
| `editable` | 当前用户是否可编辑（是否为任务处理人） |
| `formId` | 当前节点绑定的表单 ID |
| `formVersion` | 表单版本 |
| `formInstanceId` | 表单实例 ID |
| `formFields` | 当前表单的**未分组**字段列表（含权限+值） |
| `groups` | 当前表单的**分组**字段列表，每组含 `name`、`sort`、`collapsed`、`fields` |
| `inherited` | 继承自主表单的完整结构（仅 inheritMainForm=1 时有值） |

### inherited 字段说明

| 字段 | 说明 |
|---|---|
| `formId` | 主表单 ID |
| `formVersion` | 主表单版本 |
| `formInstanceId` | 主表单实例 ID |
| `formFields` | 主表单的未分组字段（含权限+值+sourceFormId） |
| `groups` | 主表单的分组字段 |

### 前端渲染顺序

1. 先渲染 `inherited`（主表单的 formFields + groups）
2. 再渲染当前表单的 `formFields` + `groups`
3. fieldKey 相同时，当前表单定义覆盖主表单定义

### editable=false 时的行为

所有字段的 `permission` 强制返回 `READONLY`，前端全部只读渲染。

### inheritMainForm=1 的字段合并

- 不在内存中合并成一个平铺列表，而是保持分组结构分开返回
- 前端按渲染顺序依次展示：先主表单分组 → 再当前表单分组
- fieldKey 去重：如果主表单和节点表单有相同 fieldKey，前端跳过主表单中的该字段

---

## 七、字段权限校验规则

### 提交时（/pass）

| 权限 | 前端传值处理 | 必填校验 |
|---|---|---|
| `EDITABLE` | 接受更新 | 否 |
| `REQUIRED` | 接受更新 | 是，必须非空 |
| `READONLY` | 忽略传值，保持原值 | 否 |
| `HIDDEN` | 忽略传值，保持原值 | 否 |
| 未配置 | 使用表单模板默认配置 | 取决于模板 |

### 草稿时（/saveDraft）

不校验必填，允许部分填写。`READONLY`/`HIDDEN` 字段同样忽略前端传值。

---

## 八、接口改动汇总

### 8.1 POST /processInstance/saveDraft

新增请求参数 `formData`（表单数据，Map<String, Object>），可选。按 startEvent 节点配置解析权限。

### 8.2 POST /processInstance/start

新增请求参数 `formData`（表单数据，Map<String, Object>），可选。按 startEvent 节点配置解析权限，无绑定表单时忽略。

### 8.3 POST /processInstance/pass

新增请求参数 `formData`（节点表单数据，Map<String, Object>），可选。无绑定表单时忽略。

### 8.4 POST /processInstance/info

响应新增 `taskForm` 字段（需传 taskId）。未传 taskId 或无绑定表单时为 null。

---

## 九、实现改动点

### 新增

| 类 | 说明 |
|---|---|
| `ProcessInstanceForm` | 关联表实体类（common） |
| `ProcessInstanceFormMapper` | 关联表 Mapper |
| `TaskFormVO` | 表单渲染数据 VO（common） |

### 修改

| 类 | 改动 |
|---|---|
| `ProcessHandleParam` | 增加 `formData` 字段 |
| `StartProcessCommand` | 创建主表单实例 + 写入数据 |
| `PassTaskCommand` | 校验字段权限 + 写入表单数据 |
| `ProcessInstanceServiceImpl.saveDraft()` | 保存主表单数据 |
| `ProcessInstanceServiceImpl.info()` | 组装 taskForm 返回 |
| `ApprovalTaskCreateListener` | 节点表单按需创建 |
| `ProcessInstanceController` | 接口参数/响应调整 |

---

## 十、注意事项

1. **版本快照**：关联表记录 `formVersion`，流程运行期间表单模板变更不影响已运行实例。
2. **驳回场景**：节点表单实例已存在时复用，不重复创建。
3. **无表单流程**：流程定义未绑定表单时，所有表单相关逻辑跳过，不影响现有流程。
4. **字段权限来源**：运行时从流程定义的发布版本（非 DRAFT）读取字段权限配置。
5. **事务一致性**：表单数据写入和任务流转在同一事务中，失败全部回滚。