# com.cat.simple.config.flowable

围绕 Flowable 引擎做的二开：把 BPMN 中 UserTask 上自定义的 `<flowable:approvalType>` 翻译为 Flowable 原生模型（多实例 / 候选人）+ 运行期监听器，统一处理"会签 / 或签 / 随机1人 / 认领"四种审批场景，并提供拒绝处理入口。

## 整体流转

```
BPMN 部署
  └─ ApprovalUserTaskParseHandler (pre-parse 阶段)
        ├─ 注册 create TaskListener  ───────────────┐
        └─ 会签/或签 → 注入 MultiInstanceLoopCharacteristics
                       (collection 用 ${candidateResolver.resolveAssignees(execution)} 延迟到运行期)
                                                    │
流程运行                                              │
  └─ 进入 UserTask                                   │
        ├─ 多实例: CandidateResolver.resolveAssignees ──┘
        │     运行期解析候选人 → List<userId> → 每人一个 task
        └─ 每个 task CREATE 事件触发 ApprovalTaskCreateListener
              └─ 按 approvalType 派发到对应 ApprovalTypeHandler
                    ├─ 1 会签 / 2 或签 → 仅日志 (assignee 已由 MI 注入)
                    ├─ 3 随机1人 → 候选池随机抽 1, setAssignee
                    └─ 4 认领 → 把候选池 addCandidateUser

任意时刻
  └─ TaskRejectService.reject(taskId) → 终止流程实例
        └─ ProcessInstanceEndListener 捕获 PROCESS_CANCELLED → 标记 TERMINATED
```

## 文件清单

### 解析期（部署时一次）

| 文件 | 作用 |
|---|---|
| `parse/ApprovalUserTaskParseHandler.java` | `BpmnParseHandler`，在 Flowable 默认 UserTaskParseHandler 之前运行。读取 `extensionElements` 里的 `approvalType` / `candidate*` / `passRate`，为带 approvalType 的 UserTask 注入 create 监听器；会签/或签额外注入多实例配置。 |
| `FlowableEngineConfigurer.java` | `EngineConfigurationConfigurer`，把 `ApprovalUserTaskParseHandler` 注册到 Flowable 引擎的 `preBpmnParseHandlers`。 |

### 运行期（每次进入节点 / 创建任务）

| 文件 | 作用 |
|---|---|
| `listener/ApprovalTaskCreateListener.java` | 由解析器注入到每个审批 UserTask 的 `create` TaskListener，Spring Bean 名 `approvalTaskCreateListener`。从 BpmnModel 读取 ApprovalContext，按 type 分发给对应 handler。 |
| `candidate/CandidateResolver.java` | Spring Bean 名 `candidateResolver`，被多实例 collection 表达式 `${candidateResolver.resolveAssignees(execution)}` 调用，把候选源（用户 / 角色 / 用户组 / 部门）合并展开成最终用户 ID 集合。若结果为空则抛异常，防止 Flowable 跳过节点。\n**v1**：`candidateUsers` 直接当用户 ID 处理，其余三个候选源 TODO 待接入。 |

### 审批策略

| 文件 | 作用 |
|---|---|
| `approval/ApprovalTypeEnum.java` | 4 种审批类型枚举：`COUNTERSIGN(1)` / `OR_SIGN(2)` / `RANDOM(3)` / `CLAIM(4)`，提供 `of(int)` / `of(String)` 静态工厂。 |
| `approval/ApprovalContext.java` | record，封装从 `extensionElements` 解析出的 6 个字段（type / candidateUsers / candidateRoles / candidateGroups / candidateDepts / passRate）。提供 `from(UserTask)` 工厂，非审批节点返回 `null`。 |
| `approval/ApprovalTypeHandler.java` | 策略接口：`supports()` 返回处理的 type，`applyOnCreate(task, ctx)` 在任务 create 时执行。 |
| `approval/handler/CountersignHandler.java` | **会签**（approvalType=1）：多实例已由解析期配置，assignee 由 MI 的 elementVariable 注入，这里仅日志。完成条件 `nrOfCompletedInstances >= nrOfInstances * passRate`。 |
| `approval/handler/OrSignHandler.java` | **或签**（approvalType=2）：多实例已由解析期配置，完成条件 `nrOfCompletedInstances >= 1`，任一实例完成即流转。 |
| `approval/handler/RandomHandler.java` | **随机1人**（approvalType=3）：调 `CandidateResolver.resolve(ctx)` 取候选池，`ThreadLocalRandom` 抽 1 人 `setAssignee`。 |
| `approval/handler/ClaimHandler.java` | **认领**（approvalType=4）：取候选池后逐个 `addCandidateUser`，不设 assignee，等待用户主动 claim。 |

### 拒绝处理

| 文件 | 作用 |
|---|---|
| `reject/TaskRejectService.java` | 拒绝入口。**v1 策略**：写入 `rejected` / `rejectReason` / `rejectedBy` 流程变量，然后 `runtimeService.deleteProcessInstance(...)` 终止整个流程实例。状态变更由 `ProcessInstanceEndListener` 监听 `PROCESS_CANCELLED` 自动标记为 TERMINATED。 |

### 流程实例结束

| 文件 | 作用 |
|---|---|
| `ProcessInstanceEndListener.java` | 监听 `PROCESS_COMPLETED` / `PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT` / `PROCESS_CANCELLED`，把流程实例状态写入业务表。审批拒绝、自然结束都走这里。 |

## 关键约定

- **BPMN 命名空间**：`xmlns:flowable="http://flowable.org/bpmn"`，审批扩展元素必须用 `flowable:` 前缀。
- **运行期延迟解析**：候选人合并放在多实例 collection 表达式里，部署后修改候选人数据（角色绑定、部门人员）能立即在新流程实例中生效，无需重新部署。
- **Bean 名约定**：
  - `candidateResolver` —— 被多实例 collection 表达式引用
  - `approvalTaskCreateListener` —— 被 TaskListener 表达式引用
  改名同步改 `ApprovalUserTaskParseHandler` 中的常量。
- **多实例变量**：elementVariable 固定为 `approvalAssignee`，UserTask 的 assignee 设为 `${approvalAssignee}`。
- **passRate 计算**：`nrOfCompletedInstances >= nrOfInstances * passRate`，避免 JUEL 整型除法精度问题。passRate=1.0 即全部通过。

## 扩展点

- **候选人展开**：`CandidateResolver.resolve` 中 3 个 TODO（角色 / 用户组 / 部门），接入 RoleService / OrgService 真实查询。
- **拒绝细化**：`TaskRejectService` 当前是"任一拒绝即终止"，会签/或签场景可演化为按反向阈值提前终止、退回上一节点等。
- **新增审批类型**：加 `ApprovalTypeEnum` 枚举值 + 实现一个 `ApprovalTypeHandler` Bean，Spring 自动注入到 `ApprovalTaskCreateListener` 的 handlerMap。如果需要解析期改写 BPMN 模型（类似多实例），还需在 `ApprovalUserTaskParseHandler.parse` 的 switch 里加分支。
