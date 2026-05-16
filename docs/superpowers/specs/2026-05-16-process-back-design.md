# 流程驳回（Back）功能设计方案

## 1. 背景与目标

当前系统的 `reject()` 仅支持终止整个流程实例（`runtimeService.deleteProcessInstance`），无法满足实际业务中「驳回到上一步」「驳回到指定节点」等常见需求。

本方案在保持 `REJECT`（拒绝/终止）作为独立操作的前提下，新增 `BACK`（驳回/回退）操作，覆盖以下场景：

- 驳回到上一节点
- 驳回到指定节点
- 用户自选历史节点驳回
- 会签/或签等多实例场景下的驳回协调

## 2. 设计原则

1. **独立操作**：`REJECT` 与 `BACK` 是两个独立操作，各自有独立的接口和轨迹记录
2. **节点配置驱动**：每个 UserTask 节点通过 BPMN 扩展元素配置自身允许的驳回方式、目标节点、回退后分配策略等
3. **审批轮次（Round）**：通过 `round` 字段区分同一节点被多次审批的记录，支持轨迹图渲染
4. **职责分离**：`ProcessBackService` 独立封装回退逻辑，`ProcessInstanceService` 保持原有职责不变

## 3. 数据模型变更

### 3.1 `cat_process_handle_info` 表

| 字段名 | 类型 | 说明 |
|-------|------|------|
| `round` | `INT` | 审批轮次，默认 1。节点被驳回后重新审批时递增 |
| `task_definition_key` | `VARCHAR(64)` | BPMN 节点 ID，用于按节点统计轮次 |
| `extra` | `VARCHAR(500)` | 扩展 JSON，back 时记录目标节点信息 |

### 3.2 `ProcessHandleInfo` 实体

```java
private Integer round = 1;
private String taskDefinitionKey;
private String extra;
```

## 4. BPMN 扩展配置模型

在现有 `ApprovalContext` 基础上新增 2 个解析字段：

```java
public record ApprovalContext(
        ApprovalTypeEnum type,
        List<String> candidateUsers,
        List<String> candidateRoles,
        List<String> candidateGroups,
        List<String> candidateDepts,
        BigDecimal passRate,
        List<String> actionButtons,
        String backType,
        String backNodeId,
        String backAssigneePolicy,        // 新增
        String multiInstanceBackPolicy    // 新增
) { }
```

### 4.1 新增常量

```java
public static final String EL_BACK_ASSIGNEE_POLICY = "backAssigneePolicy";
public static final String EL_MULTI_INSTANCE_BACK_POLICY = "multiInstanceBackPolicy";
```

### 4.2 配置枚举值

| 配置项 | 可选值 | 语义 |
|-------|--------|------|
| `backType` | `prev` / `specific` / `choose` | 当前节点允许的驳回方式 |
| `backNodeId` | 节点 ID | `backType=specific` 时必填 |
| `backAssigneePolicy` | `auto` / `last_handler` / `reassign` | 回退到目标节点后的任务分配策略 |
| `multiInstanceBackPolicy` | `auto` / `all_back` / `independent` | 多实例下一人 back 对其他实例的影响 |

#### `backAssigneePolicy` 详解

| 值 | 行为 |
|---|------|
| `auto`（默认） | 目标节点有历史办理记录 → 派给上次办理人；无记录 → 按节点配置重新分配 |
| `last_handler` | 强制派给上次处理过该节点的人 |
| `reassign` | 按节点 candidate 配置重新解析分配 |

#### `multiInstanceBackPolicy` 详解

| 值 | 行为 |
|---|------|
| `auto`（默认） | 会签 → `all_back`；或签/随机/认领 → 首期同样按 `all_back` 处理 |
| `all_back` | 任意子实例 back，取消该节点所有并行实例，整体回退 |
| `independent` | 仅处理当前子实例，其他实例不受影响。**首期暂不实现** |

### 4.3 `ApprovalTaskCreateListener` 补充

在任务创建时，将新增配置写入任务局部变量：

```java
if (ctx.backAssigneePolicy() != null) {
    delegateTask.setVariableLocal(EL_BACK_ASSIGNEE_POLICY, ctx.backAssigneePolicy());
}
if (ctx.multiInstanceBackPolicy() != null) {
    delegateTask.setVariableLocal(EL_MULTI_INSTANCE_BACK_POLICY, ctx.multiInstanceBackPolicy());
}
```

## 5. API 设计

### 5.1 Controller 层

在 `ProcessInstanceController` 中新增：

```java
@Operation(summary = "驳回")
@RequestMapping(value = "/back", method = RequestMethod.POST)
public HttpResult<?> back(@RequestBody ProcessBackParam param) {
    processBackService.back(param);
    return HttpResult.back(HttpResultStatus.SUCCESS);
}

@Operation(summary = "查询当前任务可驳回的目标节点")
@RequestMapping(value = "/availableBackTargets", method = RequestMethod.POST)
public HttpResult<List<BackTargetNode>> availableBackTargets(@RequestParam String taskId) {
    return HttpResult.back(processBackService.getAvailableBackTargets(taskId));
}
```

> `reject` 保持现有终止流程逻辑，不做任何改动。

### 5.2 参数与返回模型

**`ProcessBackParam`**：

```java
@Data
public class ProcessBackParam {
    private Integer processInstanceId;
    private String taskId;
    private String remark;
    private String targetNodeId;    // backType=choose 时必填
}
```

**`BackTargetNode`**：

```java
@Data
public class BackTargetNode {
    private String nodeId;
    private String nodeName;
}
```

**`BackConfig`**：

```java
@Data
public class BackConfig {
    private boolean allowBack;
    private String backType;
    private String backNodeId;
    private String backAssigneePolicy;
    private String multiInstanceBackPolicy;
    private List<String> actionButtons;
}
```

### 5.3 `ProcessBackService` 接口

```java
public interface ProcessBackService {
    void back(ProcessBackParam param);
    List<BackTargetNode> getAvailableBackTargets(String taskId);
    BackConfig getBackConfig(String taskId);
}
```

## 6. 核心执行逻辑

### 6.1 `back()` 执行流程

1. **参数校验**：实例存在且为 ACTIVE；当前用户有任务办理权限
2. **读取节点配置**：从任务局部变量读取 `backType`、`backNodeId`、`backAssigneePolicy`、`multiInstanceBackPolicy`
3. **解析目标节点**：
   - `prev`：查询历史活动实例，取最近完成的 UserTask
   - `specific`：使用配置的 `backNodeId`
   - `choose`：校验入参 `targetNodeId` 是否为历史有效节点
4. **多实例协调**（详见 6.2）
5. **Flowable 状态变更**：使用 `ChangeActivityStateBuilder` 改状态到目标节点
6. **目标节点任务分派**：按 `backAssigneePolicy` 设置新任务的 assignee
7. **写入轨迹记录**：`handleType=BACK`，计算并记录 `round`

### 6.2 多实例回退处理

**`all_back` 策略：**

```java
Execution miBodyExecution = runtimeService.createExecutionQuery()
    .processInstanceId(flowableInstanceId)
    .activityId(task.getTaskDefinitionKey())
    .list().stream()
    .filter(e -> e.getParentId() != null)
    .findFirst()
    .map(e -> runtimeService.createExecutionQuery()
        .executionId(e.getParentId()).singleResult())
    .orElseThrow(...);

runtimeService.createChangeActivityStateBuilder()
    .processInstanceId(flowableInstanceId)
    .moveExecutionToActivityId(miBodyExecution.getId(), targetNodeId)
    .changeState();
```

通过多实例父执行（MI Body）改状态，所有子实例自动取消。

**`independent` 策略：** 首期暂不实现，调用时抛 `UnsupportedOperationException`。

### 6.3 目标节点任务分派

`changeState()` 后，Flowable 会在目标节点创建新任务。查询到新任务后按策略指派：

| 策略 | 逻辑 |
|------|------|
| `last_handler` | 查 `process_handle_info`，取该节点 `taskDefinitionKey` 最近一条记录的 `handleUser` |
| `reassign` | 调用 `candidateResolver.resolveAssignees()` 重新分配 |
| `auto` | 有历史记录 → `last_handler`；无 → `reassign` |

### 6.4 `round` 轮次计算

```java
Integer maxRound = processHandleInfoMapper.selectMaxRound(
    processInstanceId, targetNodeId);
int newRound = (maxRound == null) ? 1 : maxRound + 1;
```

目标节点重新被审批时（pass/claim），新记录复用该 `newRound`，形成完整轮次链路。

## 7. 多实例回退行为矩阵（首期）

| 审批类型 | `multiInstanceBackPolicy` | 首期行为 |
|---------|--------------------------|---------|
| 会签 COUNTERSIGN | `auto` | 映射为 `all_back` |
| 会签 COUNTERSIGN | `all_back` | 一人 back，取消所有并行实例，整体回退 |
| 会签 COUNTERSIGN | `independent` | 抛 `UnsupportedOperationException` |
| 或签 OR_SIGN | `auto` | 映射为 `all_back` |
| 或签 OR_SIGN | `all_back` | 一人 back，取消所有并行实例，整体回退 |
| 或签 OR_SIGN | `independent` | 抛 `UnsupportedOperationException` |
| 随机 RANDOM | — | 单实例，直接改状态 |
| 认领 CLAIM | — | 单实例，直接改状态 |

## 8. 异常处理

| 场景 | 处理 |
|------|------|
| 流程实例非 ACTIVE | `IllegalStateException` |
| 当前用户非任务办理人 | `IllegalStateException` |
| 目标节点不存在 | `IllegalArgumentException` |
| 目标节点当前有进行中任务 | `IllegalStateException`，防止重复任务 |
| `backType=choose` 未传 `targetNodeId` | `IllegalArgumentException` |
| `backType=specific` 未配置 `backNodeId` | `IllegalStateException` |
| `independent` 策略（首期） | `UnsupportedOperationException` |
| Flowable 状态变更失败 | 捕获异常，包装为业务异常，事务回滚 |
| 并发驳回 | `@Transactional` 保证串行执行 |

## 9. 事务边界

`back()` 方法标记 `@Transactional(rollbackFor = Exception.class)`，确保：
1. Flowable 执行状态变更
2. 新任务分派
3. `ProcessHandleInfo` 记录写入

三者原子性提交或回滚。

## 10. 范围说明

### 首期实现

- `backType = prev / specific / choose`
- `backAssigneePolicy = auto / last_handler / reassign`
- `multiInstanceBackPolicy = auto / all_back`
- 单实例节点直接回退
- 多实例节点 `all_back` 回退

### 二期扩展

- `multiInstanceBackPolicy = independent`（多实例独立回退）
- 驳回后邮件/消息通知
- 驳回原因必填校验（可配置）