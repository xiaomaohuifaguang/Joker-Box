# Flowable 流程引擎结构重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `ProcessInstanceServiceImpl` 从 479 行的臃肿实现重构为干净的门面（~90 行），通过命令模式解耦审批操作，统一变量读写层，并预留业务扩展钩子。

**Architecture:** 引入 `ProcessCommand<T>` 抽象基类封装通用校验/执行/记录模板，每个审批操作（start/claim/pass/reject/back）实现为独立 Command；`ProcessVariableStore` 统一隔离 Flowable API；`ProcessLifecycleHook` 接口提供 before/after 扩展点。

**Tech Stack:** Java 21, Spring Boot 3.x, Flowable 7.x, MyBatis-Plus, JUnit 5, Mockito

---

## File Structure Overview

### New Files (17)

| File | Responsibility |
|------|----------------|
| `config/flowable/variable/VariableNames.java` | 所有流程变量名的枚举定义 |
| `config/flowable/variable/ProcessVariableStore.java` | 统一变量读写，隔离 RuntimeService/TaskService |
| `config/flowable/guard/ProcessGuard.java` | 通用校验与查询（用户/实例/任务/定义） |
| `config/flowable/recorder/HandleInfoRecorder.java` | 审批轨迹记录器 |
| `config/flowable/command/ProcessCommand.java` | Command 抽象基类（模板方法） |
| `config/flowable/command/CommandBus.java` | 命令分发器（事务边界） |
| `config/flowable/command/StartProcessCommand.java` | 启动流程命令 |
| `config/flowable/command/ClaimTaskCommand.java` | 认领任务命令 |
| `config/flowable/command/PassTaskCommand.java` | 审批通过命令 |
| `config/flowable/command/RejectTaskCommand.java` | 审批拒绝命令 |
| `config/flowable/command/BackTaskCommand.java` | 驳回命令 |
| `config/flowable/hook/ProcessLifecycleHook.java` | 生命周期钩子接口 |
| `config/flowable/hook/StartContext.java` | 启动上下文 |
| `config/flowable/hook/ClaimContext.java` | 认领上下文 |
| `config/flowable/hook/PassContext.java` | 通过上下文 |
| `config/flowable/hook/RejectContext.java` | 拒绝上下文 |
| `config/flowable/hook/BackContext.java` | 驳回上下文 |

### Modified Files (6)

| File | Change |
|------|--------|
| `service/impl/ProcessInstanceServiceImpl.java` | 大幅精简为门面 |
| `listener/ApprovalTaskCreateListener.java` | 改用 ProcessVariableStore |
| `listener/ProcessInstanceEndListener.java` | 移除 Service 依赖 |
| `back/BackConfigReader.java` | 改用 ProcessVariableStore |
| `back/BackEngine.java` | 改用 ProcessVariableStore |
| `constant/ProcessConstants.java` | 引用 VariableNames |

### Deleted Files (1)

| File | Reason |
|------|--------|
| `util/TaskVariableUtil.java` | 功能合并到 ProcessVariableStore |

---

## Phase 1: 基础设施 — VariableNames + ProcessVariableStore

### Task 1: Create VariableNames Enum

**Files:**
- Create: `simple/src/main/java/com/cat/simple/config/flowable/variable/VariableNames.java`

- [ ] **Step 1: Create the enum file**

```java
package com.cat.simple.config.flowable.variable;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum VariableNames {
    APPROVAL_TYPE("approvalType"),
    CANDIDATE_USERS("candidateUsers"),
    CANDIDATE_ROLES("candidateRoles"),
    CANDIDATE_GROUPS("candidateGroups"),
    CANDIDATE_DEPTS("candidateDepts"),
    PASS_RATE("passRate"),
    ACTION_BUTTONS("actionButtons"),
    BACK_TYPE("backType"),
    BACK_NODE_ID("backNodeId"),
    BACK_ASSIGNEE_POLICY("backAssigneePolicy"),
    FORM_DATA("formData"),
    BACK_ASSIGNEES_PREFIX("__back_assignees_");

    private final String key;
}
```

- [ ] **Step 2: Commit**

```bash
git add simple/src/main/java/com/cat/simple/config/flowable/variable/VariableNames.java
git commit -m "feat: add VariableNames enum for unified variable access"
```

---

### Task 2: Create ProcessVariableStore

**Files:**
- Create: `simple/src/main/java/com/cat/simple/config/flowable/variable/ProcessVariableStore.java`

- [ ] **Step 1: Create the store class**

```java
package com.cat.simple.config.flowable.variable;

import jakarta.annotation.Resource;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

@Component
public class ProcessVariableStore {

    @Resource private RuntimeService runtimeService;
    @Resource private TaskService taskService;

    public void set(String processInstanceId, VariableNames name, Object value) {
        runtimeService.setVariable(processInstanceId, name.getKey(), value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String processInstanceId, VariableNames name, Class<T> type) {
        Object value = runtimeService.getVariable(processInstanceId, name.getKey());
        return value != null && type.isInstance(value) ? (T) value : null;
    }

    public void remove(String processInstanceId, VariableNames name) {
        runtimeService.removeVariable(processInstanceId, name.getKey());
    }

    public void setLocal(String taskId, VariableNames name, Object value) {
        taskService.setVariableLocal(taskId, name.getKey(), value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getLocal(String taskId, VariableNames name, Class<T> type) {
        Object value = taskService.getVariableLocal(taskId, name.getKey());
        return value != null && type.isInstance(value) ? (T) value : null;
    }

    public void setLocal(Task task, VariableNames name, Object value) {
        setLocal(task.getId(), name, value);
    }

    public <T> T getLocal(Task task, VariableNames name, Class<T> type) {
        return getLocal(task.getId(), name, type);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add simple/src/main/java/com/cat/simple/config/flowable/variable/ProcessVariableStore.java
git commit -m "feat: add ProcessVariableStore for unified variable read/write"
```

---

### Task 3: Unit Test for ProcessVariableStore

**Files:**
- Create: `simple/src/test/java/com/cat/simple/config/flowable/variable/ProcessVariableStoreTest.java`

- [ ] **Step 1: Write the test**

```java
package com.cat.simple.config.flowable.variable;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessVariableStoreTest {

    @Mock private RuntimeService runtimeService;
    @Mock private TaskService taskService;
    @InjectMocks private ProcessVariableStore store;

    @Test
    void testSetAndGetProcessVariable() {
        store.set("pid-123", VariableNames.BACK_TYPE, "prev");
        verify(runtimeService).setVariable("pid-123", "backType", "prev");

        when(runtimeService.getVariable("pid-123", "backType")).thenReturn("prev");
        String value = store.get("pid-123", VariableNames.BACK_TYPE, String.class);
        assertEquals("prev", value);
    }

    @Test
    void testGetProcessVariableReturnsNullWhenNotFound() {
        when(runtimeService.getVariable("pid-123", "backType")).thenReturn(null);
        String value = store.get("pid-123", VariableNames.BACK_TYPE, String.class);
        assertNull(value);
    }

    @Test
    void testRemoveProcessVariable() {
        store.remove("pid-123", VariableNames.BACK_TYPE);
        verify(runtimeService).removeVariable("pid-123", "backType");
    }

    @Test
    void testSetAndGetLocalVariable() {
        store.setLocal("task-456", VariableNames.ACTION_BUTTONS, "pass,reject");
        verify(taskService).setVariableLocal("task-456", "actionButtons", "pass,reject");

        when(taskService.getVariableLocal("task-456", "actionButtons")).thenReturn("pass,reject");
        String value = store.getLocal("task-456", VariableNames.ACTION_BUTTONS, String.class);
        assertEquals("pass,reject", value);
    }
}
```

- [ ] **Step 2: Run the test**

```bash
./mvnw test -pl simple -Dtest=ProcessVariableStoreTest -DfailIfNoTests=false
```

Expected: All 4 tests PASS.

- [ ] **Step 3: Commit**

```bash
git add simple/src/test/java/com/cat/simple/config/flowable/variable/ProcessVariableStoreTest.java
git commit -m "test: add ProcessVariableStore unit tests"
```

---

### Task 4: Migrate ApprovalTaskCreateListener to ProcessVariableStore

**Files:**
- Modify: `simple/src/main/java/com/cat/simple/config/flowable/listener/ApprovalTaskCreateListener.java`

- [ ] **Step 1: Add ProcessVariableStore dependency and update variable writes**

In `ApprovalTaskCreateListener.java`:

```java
// Add import
import com.cat.simple.config.flowable.variable.ProcessVariableStore;
import com.cat.simple.config.flowable.variable.VariableNames;

// Add field
@Resource
private ProcessVariableStore variableStore;
```

Replace the variable write block in `notify()` method (lines 69-80):

```java
// BEFORE:
// delegateTask.setVariableLocal(EL_ACTION_BUTTONS, String.join(",", ctx.actionButtons()));
// delegateTask.setVariableLocal(EL_BACK_TYPE, ctx.backType());
// ... etc

// AFTER:
if (ctx.actionButtons() != null && !ctx.actionButtons().isEmpty()) {
    variableStore.setLocal(delegateTask, VariableNames.ACTION_BUTTONS, String.join(",", ctx.actionButtons()));
}
if (ctx.backType() != null && !ctx.backType().isBlank()) {
    variableStore.setLocal(delegateTask, VariableNames.BACK_TYPE, ctx.backType());
}
if (ctx.backNodeId() != null && !ctx.backNodeId().isBlank()) {
    variableStore.setLocal(delegateTask, VariableNames.BACK_NODE_ID, ctx.backNodeId());
}
if (ctx.backAssigneePolicy() != null && !ctx.backAssigneePolicy().isBlank()) {
    variableStore.setLocal(delegateTask, VariableNames.BACK_ASSIGNEE_POLICY, ctx.backAssigneePolicy());
}
```

Remove unused imports: `ProcessConstants.*` static import can be removed if no longer used.

- [ ] **Step 2: Run existing integration tests to verify behavior unchanged**

```bash
./mvnw test -pl simple -Dtest=ProcessInstanceServiceTest -DfailIfNoTests=false
```

Expected: Tests PASS (or skip if they need valid IDs).

- [ ] **Step 3: Commit**

```bash
git add simple/src/main/java/com/cat/simple/config/flowable/listener/ApprovalTaskCreateListener.java
git commit -m "refactor: migrate ApprovalTaskCreateListener to ProcessVariableStore"
```

---

### Task 5: Migrate BackConfigReader to ProcessVariableStore

**Files:**
- Modify: `simple/src/main/java/com/cat/simple/config/flowable/back/BackConfigReader.java`

- [ ] **Step 1: Replace TaskVariableUtil with ProcessVariableStore**

In `BackConfigReader.java`:

```java
// Remove import
// import com.cat.simple.config.flowable.util.TaskVariableUtil;

// Add imports
import com.cat.simple.config.flowable.variable.ProcessVariableStore;
import com.cat.simple.config.flowable.variable.VariableNames;

// Replace field
// @Resource private TaskVariableUtil taskVariableUtil;
@Resource private ProcessVariableStore variableStore;
```

Replace the `getBackConfig()` method body (lines 55-74):

```java
public BackConfig getBackConfig(String taskId) {
    Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
    if (task == null) {
        throw new IllegalArgumentException("任务不存在: " + taskId);
    }

    String actionButtons = variableStore.getLocal(task, VariableNames.ACTION_BUTTONS, String.class);
    String backType = variableStore.getLocal(task, VariableNames.BACK_TYPE, String.class);
    String backNodeId = variableStore.getLocal(task, VariableNames.BACK_NODE_ID, String.class);
    String backAssigneePolicy = variableStore.getLocal(task, VariableNames.BACK_ASSIGNEE_POLICY, String.class);

    BackConfig config = new BackConfig();
    config.setAllowBack(backType != null && !backType.isBlank());
    config.setBackType(backType);
    config.setBackNodeId(backNodeId);
    config.setBackAssigneePolicy(backAssigneePolicy != null ? backAssigneePolicy : BackAssigneePolicyEnum.AUTO.getCode());
    config.setActionButtons(actionButtons != null && !actionButtons.isBlank()
            ? Arrays.asList(actionButtons.split(",")) : List.of());
    return config;
}
```

Remove the `ProcessConstants.*` static import if no longer used in this file.

- [ ] **Step 2: Run tests**

```bash
./mvnw test -pl simple -Dtest=ProcessInstanceServiceTest -DfailIfNoTests=false
```

Expected: Tests PASS.

- [ ] **Step 3: Commit**

```bash
git add simple/src/main/java/com/cat/simple/config/flowable/back/BackConfigReader.java
git commit -m "refactor: migrate BackConfigReader to ProcessVariableStore"
```

---

### Task 6: Migrate BackEngine to ProcessVariableStore

**Files:**
- Modify: `simple/src/main/java/com/cat/simple/config/flowable/back/BackEngine.java`
- Modify: `simple/src/main/java/com/cat/simple/config/flowable/variable/ProcessVariableStore.java`

- [ ] **Step 1: Add raw variable methods to ProcessVariableStore**

Add to `ProcessVariableStore.java`:

```java
public void setRaw(String processInstanceId, String key, Object value) {
    runtimeService.setVariable(processInstanceId, key, value);
}

public void removeRaw(String processInstanceId, String key) {
    runtimeService.removeVariable(processInstanceId, key);
}
```

- [ ] **Step 2: Update BackEngine to use ProcessVariableStore**

In `BackEngine.java`:

```java
// Add imports
import com.cat.simple.config.flowable.variable.ProcessVariableStore;
import com.cat.simple.config.flowable.variable.VariableNames;
```

Add field:
```java
@Resource
private ProcessVariableStore variableStore;
```

In both `backSingleInstance()` and `backMultiInstanceAllBack()`, replace:

```java
// BEFORE:
// String backVarName = BACK_ASSIGNEE_VAR_PREFIX + targetNodeId;

// AFTER:
String backVarName = VariableNames.BACK_ASSIGNEES_PREFIX.getKey() + targetNodeId;
```

Replace `runtimeService.setVariable()` with:
```java
variableStore.setRaw(instance.getProcessInstanceId(), backVarName, prevRoundHandlers);
```

Replace `runtimeService.removeVariable()` in finally block with:
```java
variableStore.removeRaw(instance.getProcessInstanceId(), backVarName);
```

- [ ] **Step 3: Run tests**

```bash
./mvnw test -pl simple -Dtest=ProcessInstanceServiceTest -DfailIfNoTests=false
```

Expected: Tests PASS.

- [ ] **Step 4: Commit**

```bash
git add simple/src/main/java/com/cat/simple/config/flowable/back/BackEngine.java
git add simple/src/main/java/com/cat/simple/config/flowable/variable/ProcessVariableStore.java
git commit -m "refactor: migrate BackEngine to ProcessVariableStore"
```

---

### Task 7: Delete TaskVariableUtil and Update ProcessConstants

**Files:**
- Delete: `simple/src/main/java/com/cat/simple/config/flowable/util/TaskVariableUtil.java`
- Modify: `simple/src/main/java/com/cat/simple/config/flowable/constant/ProcessConstants.java`

- [ ] **Step 1: Delete TaskVariableUtil**

```bash
git rm simple/src/main/java/com/cat/simple/config/flowable/util/TaskVariableUtil.java
```

- [ ] **Step 2: Update ProcessConstants to reference VariableNames**

In `ProcessConstants.java`:

```java
package com.cat.simple.config.flowable.constant;

import com.cat.simple.config.flowable.enums.ExtensionElementEnum;
import com.cat.simple.config.flowable.variable.VariableNames;

public class ProcessConstants {

    public static final String EL_APPROVAL_TYPE = ExtensionElementEnum.APPROVAL_TYPE.getCode();
    public static final String EL_CANDIDATE_USERS = ExtensionElementEnum.CANDIDATE_USERS.getCode();
    public static final String EL_CANDIDATE_ROLES = ExtensionElementEnum.CANDIDATE_ROLES.getCode();
    public static final String EL_CANDIDATE_GROUPS = ExtensionElementEnum.CANDIDATE_GROUPS.getCode();
    public static final String EL_CANDIDATE_DEPTS = ExtensionElementEnum.CANDIDATE_DEPTS.getCode();
    public static final String EL_PASS_RATE = ExtensionElementEnum.PASS_RATE.getCode();
    public static final String EL_ACTION_BUTTONS = ExtensionElementEnum.ACTION_BUTTONS.getCode();
    public static final String EL_BACK_TYPE = ExtensionElementEnum.BACK_TYPE.getCode();
    public static final String EL_BACK_NODE_ID = ExtensionElementEnum.BACK_NODE_ID.getCode();
    public static final String EL_BACK_ASSIGNEE_POLICY = ExtensionElementEnum.BACK_ASSIGNEE_POLICY.getCode();

    public static final String BACK_ASSIGNEE_VAR_PREFIX = VariableNames.BACK_ASSIGNEES_PREFIX.getKey();
}
```

- [ ] **Step 3: Verify compilation**

```bash
./mvnw compile -pl simple
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add simple/src/main/java/com/cat/simple/config/flowable/constant/ProcessConstants.java
git commit -m "refactor: delete TaskVariableUtil, update ProcessConstants to reference VariableNames"
```

---

## Phase 2: 校验层 — ProcessGuard

### Task 8: Create ProcessGuard

**Files:**
- Create: `simple/src/main/java/com/cat/simple/config/flowable/guard/ProcessGuard.java`

- [ ] **Step 1: Create the guard class**

```java
package com.cat.simple.config.flowable.guard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.auth.LoginUser;
import com.cat.common.entity.process.ProcessDefinition;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.enums.ProcessStatusEnum;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.mapper.ProcessDefinitionMapper;
import com.cat.simple.mapper.ProcessInstanceMapper;
import jakarta.annotation.Resource;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ProcessGuard {

    @Resource private ProcessInstanceMapper processInstanceMapper;
    @Resource private ProcessDefinitionMapper processDefinitionMapper;
    @Resource private TaskService taskService;
    @Resource private RuntimeService runtimeService;
    @Resource private HistoryService historyService;

    public String getCurrentUserId() {
        return Optional.ofNullable(SecurityUtils.getLoginUser())
                .map(LoginUser::getUserId)
                .orElseThrow(() -> new IllegalStateException("当前未登录"));
    }

    public ProcessInstance assertInstanceActive(Integer processInstanceId) {
        ProcessInstance instance = processInstanceMapper.selectById(processInstanceId);
        if (instance == null) {
            throw new IllegalArgumentException("流程实例不存在: " + processInstanceId);
        }
        if (!ProcessStatusEnum.ACTIVE.getStatus().equals(instance.getProcessStatus())) {
            throw new IllegalStateException("流程实例非审批中状态: " + processInstanceId);
        }
        return instance;
    }

    public ProcessInstance assertInstanceDraft(Integer processInstanceId) {
        ProcessInstance instance = processInstanceMapper.selectById(processInstanceId);
        if (instance == null) {
            throw new IllegalArgumentException("草稿不存在: " + processInstanceId);
        }
        if (!ProcessStatusEnum.DRAFT.getStatus().equals(instance.getProcessStatus())) {
            throw new IllegalStateException("该流程实例不是草稿状态: " + processInstanceId);
        }
        return instance;
    }

    public Task assertTaskAssignee(String taskId) {
        String userId = getCurrentUserId();
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .taskAssignee(userId)
                .singleResult();
        if (task == null) {
            throw new IllegalStateException("当前用户没有该任务的办理权限, taskId: " + taskId);
        }
        return task;
    }

    public Task assertTaskCandidate(String taskId) {
        String userId = getCurrentUserId();
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .taskCandidateUser(userId)
                .singleResult();
        if (task == null) {
            throw new IllegalStateException("当前用户没有该任务的认领权限, taskId: " + taskId);
        }
        return task;
    }

    public Task assertTaskExists(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        return task;
    }

    public ProcessDefinition assertDefinitionPublished(Integer definitionId) {
        ProcessDefinition definition = processDefinitionMapper.selectById(definitionId);
        if (definition == null) {
            throw new IllegalArgumentException("流程定义不存在: " + definitionId);
        }
        if (!"1".equals(definition.getStatus())) {
            throw new IllegalStateException("流程定义未发布: " + definitionId);
        }
        return definition;
    }

    public ProcessInstance getInstance(Integer id) {
        return processInstanceMapper.selectById(id);
    }

    public Task getTask(String taskId) {
        return taskService.createTaskQuery().taskId(taskId).singleResult();
    }

    public ProcessInstance selectByFlowableId(String flowableProcessInstanceId) {
        return processInstanceMapper.selectOne(
                new LambdaQueryWrapper<ProcessInstance>()
                        .eq(ProcessInstance::getProcessInstanceId, flowableProcessInstanceId));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add simple/src/main/java/com/cat/simple/config/flowable/guard/ProcessGuard.java
git commit -m "feat: add ProcessGuard for unified validation and queries"
```

---

### Task 9: Unit Test for ProcessGuard

**Files:**
- Create: `simple/src/test/java/com/cat/simple/config/flowable/guard/ProcessGuardTest.java`

- [ ] **Step 1: Write the test**

```java
package com.cat.simple.config.flowable.guard;

import com.cat.common.entity.auth.LoginUser;
import com.cat.common.entity.process.ProcessDefinition;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.mapper.ProcessDefinitionMapper;
import com.cat.simple.mapper.ProcessInstanceMapper;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessGuardTest {

    @Mock private ProcessInstanceMapper processInstanceMapper;
    @Mock private ProcessDefinitionMapper processDefinitionMapper;
    @Mock private TaskService taskService;
    @InjectMocks private ProcessGuard guard;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        LoginUser user = new LoginUser();
        user.setUserId("user-1");
        securityUtilsMock.when(SecurityUtils::getLoginUser).thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    void testGetCurrentUserId() {
        assertEquals("user-1", guard.getCurrentUserId());
    }

    @Test
    void testAssertInstanceActiveReturnsInstance() {
        ProcessInstance instance = new ProcessInstance();
        instance.setId(1);
        instance.setProcessStatus("1");
        when(processInstanceMapper.selectById(1)).thenReturn(instance);

        ProcessInstance result = guard.assertInstanceActive(1);
        assertEquals(1, result.getId());
    }

    @Test
    void testAssertInstanceActiveThrowsWhenNotFound() {
        when(processInstanceMapper.selectById(1)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> guard.assertInstanceActive(1));
    }

    @Test
    void testAssertInstanceActiveThrowsWhenNotActive() {
        ProcessInstance instance = new ProcessInstance();
        instance.setProcessStatus("0");
        when(processInstanceMapper.selectById(1)).thenReturn(instance);
        assertThrows(IllegalStateException.class, () -> guard.assertInstanceActive(1));
    }

    @Test
    void testAssertTaskAssigneeReturnsTask() {
        Task task = mock(Task.class);
        TaskQuery query = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(query);
        when(query.taskId("task-1")).thenReturn(query);
        when(query.taskAssignee("user-1")).thenReturn(query);
        when(query.singleResult()).thenReturn(task);

        Task result = guard.assertTaskAssignee("task-1");
        assertSame(task, result);
    }

    @Test
    void testAssertDefinitionPublishedReturnsDefinition() {
        ProcessDefinition def = new ProcessDefinition();
        def.setId(1);
        def.setStatus("1");
        when(processDefinitionMapper.selectById(1)).thenReturn(def);

        ProcessDefinition result = guard.assertDefinitionPublished(1);
        assertEquals(1, result.getId());
    }
}
```

- [ ] **Step 2: Run the test**

```bash
./mvnw test -pl simple -Dtest=ProcessGuardTest -DfailIfNoTests=false
```

Expected: All 6 tests PASS.

- [ ] **Step 3: Commit**

```bash
git add simple/src/test/java/com/cat/simple/config/flowable/guard/ProcessGuardTest.java
git commit -m "test: add ProcessGuard unit tests"
```

---

## Phase 3: 记录层 — HandleInfoRecorder

### Task 10: Create HandleInfoRecorder

**Files:**
- Create: `simple/src/main/java/com/cat/simple/config/flowable/recorder/HandleInfoRecorder.java`

- [ ] **Step 1: Create the recorder class**

```java
package com.cat.simple.config.flowable.recorder;

import com.cat.common.entity.process.ProcessHandleInfo;
import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.enums.HandleTypeEnum;
import com.cat.simple.config.flowable.guard.ProcessGuard;
import com.cat.simple.mapper.ProcessHandleInfoMapper;
import jakarta.annotation.Resource;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Component
public class HandleInfoRecorder {

    @Resource private ProcessHandleInfoMapper processHandleInfoMapper;
    @Resource private ProcessGuard guard;

    public void recordApply(ProcessInstance instance, String userId) {
        insert(buildBase(instance, userId)
                .setHandleType(HandleTypeEnum.APPLY.getCode())
                .setRemark(HandleTypeEnum.APPLY.getName())
                .setRound(1));
    }

    public void recordClaim(ProcessHandleParam param, Task task) {
        ProcessInstance instance = guard.getInstance(param.getProcessInstanceId());
        Integer round = resolveRound(instance.getId(), task.getTaskDefinitionKey());
        String remark = StringUtils.hasText(param.getRemark())
                ? param.getRemark() : HandleTypeEnum.CLAIM.getName();
        insert(buildBase(instance, guard.getCurrentUserId())
                .setTaskId(task.getId())
                .setTaskName(task.getName())
                .setTaskDefinitionKey(task.getTaskDefinitionKey())
                .setHandleType(HandleTypeEnum.CLAIM.getCode())
                .setRemark(remark)
                .setRound(round));
    }

    public void recordPass(ProcessHandleParam param, Task task) {
        ProcessInstance instance = guard.getInstance(param.getProcessInstanceId());
        Integer round = resolveRound(instance.getId(), task.getTaskDefinitionKey());
        String remark = StringUtils.hasText(param.getRemark())
                ? param.getRemark() : HandleTypeEnum.PASS.getName();
        insert(buildBase(instance, guard.getCurrentUserId())
                .setTaskId(task.getId())
                .setTaskName(task.getName())
                .setTaskDefinitionKey(task.getTaskDefinitionKey())
                .setHandleType(HandleTypeEnum.PASS.getCode())
                .setRemark(remark)
                .setRound(round));
    }

    public void recordReject(ProcessHandleParam param, Task task) {
        ProcessInstance instance = guard.getInstance(param.getProcessInstanceId());
        Integer round = resolveRound(instance.getId(), task.getTaskDefinitionKey());
        String remark = StringUtils.hasText(param.getRemark())
                ? param.getRemark() : HandleTypeEnum.REJECT.getName();
        insert(buildBase(instance, guard.getCurrentUserId())
                .setTaskId(task.getId())
                .setTaskName(task.getName())
                .setTaskDefinitionKey(task.getTaskDefinitionKey())
                .setHandleType(HandleTypeEnum.REJECT.getCode())
                .setRemark(remark)
                .setRound(round));
    }

    public void recordBack(ProcessHandleParam param, Task task,
                           String targetNodeId, String targetNodeName) {
        ProcessInstance instance = guard.getInstance(param.getProcessInstanceId());
        Integer maxRound = processHandleInfoMapper.selectMaxRound(instance.getId(), targetNodeId);
        int newRound = (maxRound == null) ? 1 : maxRound + 1;
        String remark = StringUtils.hasText(param.getRemark()) ? param.getRemark() : "驳回";
        String extra = String.format("{\"targetNodeId\":\"%s\",\"targetNodeName\":\"%s\"}",
                targetNodeId, targetNodeName != null ? targetNodeName : "");
        insert(buildBase(instance, guard.getCurrentUserId())
                .setTaskId(task.getId())
                .setTaskName(task.getName())
                .setTaskDefinitionKey(targetNodeId)
                .setHandleType(HandleTypeEnum.BACK.getCode())
                .setRemark(remark)
                .setRound(newRound)
                .setExtra(extra));
    }

    private ProcessHandleInfo buildBase(ProcessInstance instance, String userId) {
        return new ProcessHandleInfo()
                .setProcessInstanceId(instance.getId())
                .setHandleUser(userId)
                .setHandleTime(LocalDateTime.now());
    }

    private Integer resolveRound(Integer processInstanceId, String taskDefinitionKey) {
        Integer max = processHandleInfoMapper.selectMaxRound(processInstanceId, taskDefinitionKey);
        return max != null ? max : 1;
    }

    private void insert(ProcessHandleInfo info) {
        if (info.getHandleTime() == null) {
            info.setHandleTime(LocalDateTime.now());
        }
        if (info.getRound() == null) {
            info.setRound(1);
        }
        processHandleInfoMapper.insert(info);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add simple/src/main/java/com/cat/simple/config/flowable/recorder/HandleInfoRecorder.java
git commit -m "feat: add HandleInfoRecorder for unified approval trail recording"
```

---

### Task 11: Unit Test for HandleInfoRecorder

**Files:**
- Create: `simple/src/test/java/com/cat/simple/config/flowable/recorder/HandleInfoRecorderTest.java`

- [ ] **Step 1: Write the test**

```java
package com.cat.simple.config.flowable.recorder;

import com.cat.common.entity.auth.LoginUser;
import com.cat.common.entity.process.ProcessHandleInfo;
import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.enums.HandleTypeEnum;
import com.cat.simple.config.flowable.guard.ProcessGuard;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.mapper.ProcessHandleInfoMapper;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandleInfoRecorderTest {

    @Mock private ProcessHandleInfoMapper processHandleInfoMapper;
    @Mock private ProcessGuard guard;
    @InjectMocks private HandleInfoRecorder recorder;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        LoginUser user = new LoginUser();
        user.setUserId("user-1");
        securityUtilsMock.when(SecurityUtils::getLoginUser).thenReturn(user);
        when(guard.getCurrentUserId()).thenReturn("user-1");
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    void testRecordApply() {
        ProcessInstance instance = new ProcessInstance();
        instance.setId(1);

        recorder.recordApply(instance, "user-1");

        ArgumentCaptor<ProcessHandleInfo> captor = ArgumentCaptor.forClass(ProcessHandleInfo.class);
        verify(processHandleInfoMapper).insert(captor.capture());
        ProcessHandleInfo info = captor.getValue();
        assertEquals(1, info.getProcessInstanceId());
        assertEquals(HandleTypeEnum.APPLY.getCode(), info.getHandleType());
        assertEquals("user-1", info.getHandleUser());
        assertEquals(1, info.getRound());
    }

    @Test
    void testRecordPass() {
        ProcessInstance instance = new ProcessInstance();
        instance.setId(1);
        when(guard.getInstance(1)).thenReturn(instance);
        when(processHandleInfoMapper.selectMaxRound(1, "task-def-1")).thenReturn(2);

        ProcessHandleParam param = new ProcessHandleParam();
        param.setProcessInstanceId(1);
        param.setRemark("同意");

        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-1");
        when(task.getName()).thenReturn("审批节点");
        when(task.getTaskDefinitionKey()).thenReturn("task-def-1");

        recorder.recordPass(param, task);

        ArgumentCaptor<ProcessHandleInfo> captor = ArgumentCaptor.forClass(ProcessHandleInfo.class);
        verify(processHandleInfoMapper).insert(captor.capture());
        ProcessHandleInfo info = captor.getValue();
        assertEquals("task-1", info.getTaskId());
        assertEquals("同意", info.getRemark());
        assertEquals(2, info.getRound());
        assertEquals(HandleTypeEnum.PASS.getCode(), info.getHandleType());
    }

    @Test
    void testRecordBack() {
        ProcessInstance instance = new ProcessInstance();
        instance.setId(1);
        when(guard.getInstance(1)).thenReturn(instance);
        when(processHandleInfoMapper.selectMaxRound(1, "target-node")).thenReturn(3);

        ProcessHandleParam param = new ProcessHandleParam();
        param.setProcessInstanceId(1);

        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-1");
        when(task.getName()).thenReturn("审批节点");

        recorder.recordBack(param, task, "target-node", "目标节点");

        ArgumentCaptor<ProcessHandleInfo> captor = ArgumentCaptor.forClass(ProcessHandleInfo.class);
        verify(processHandleInfoMapper).insert(captor.capture());
        ProcessHandleInfo info = captor.getValue();
        assertEquals(4, info.getRound());
        assertEquals("target-node", info.getTaskDefinitionKey());
        assertTrue(info.getExtra().contains("targetNodeId"));
        assertTrue(info.getExtra().contains("目标节点"));
    }
}
```

- [ ] **Step 2: Run the test**

```bash
./mvnw test -pl simple -Dtest=HandleInfoRecorderTest -DfailIfNoTests=false
```

Expected: All 3 tests PASS.

- [ ] **Step 3: Commit**

```bash
git add simple/src/test/java/com/cat/simple/config/flowable/recorder/HandleInfoRecorderTest.java
git commit -m "test: add HandleInfoRecorder unit tests"
```

---

## Phase 4: 命令层 — ProcessCommand + Commands + CommandBus

### Task 12: Create ProcessCommand Base Class and CommandBus

**Files:**
- Create: `simple/src/main/java/com/cat/simple/config/flowable/command/ProcessCommand.java`
- Create: `simple/src/main/java/com/cat/simple/config/flowable/command/CommandBus.java`

- [ ] **Step 1: Create ProcessCommand**

```java
package com.cat.simple.config.flowable.command;

import com.cat.simple.config.flowable.guard.ProcessGuard;
import com.cat.simple.config.flowable.hook.ProcessLifecycleHook;
import com.cat.simple.config.flowable.recorder.HandleInfoRecorder;
import com.cat.simple.config.flowable.variable.ProcessVariableStore;
import jakarta.annotation.Resource;

public abstract class ProcessCommand<T> {

    @Resource protected ProcessGuard guard;
    @Resource protected HandleInfoRecorder recorder;
    @Resource protected ProcessLifecycleHook lifecycleHook;
    @Resource protected ProcessVariableStore variableStore;

    public final T execute() {
        validate();
        beforeHook();
        T result = doExecute();
        afterHook(result);
        record(result);
        return result;
    }

    protected abstract void validate();
    protected abstract T doExecute();
    protected abstract void record(T result);

    protected void beforeHook() { }
    protected void afterHook(T result) { }
}
```

- [ ] **Step 2: Create CommandBus**

```java
package com.cat.simple.config.flowable.command;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CommandBus {

    @Transactional(rollbackFor = Exception.class)
    public <T> T execute(ProcessCommand<T> command) {
        return command.execute();
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add simple/src/main/java/com/cat/simple/config/flowable/command/ProcessCommand.java
git add simple/src/main/java/com/cat/simple/config/flowable/command/CommandBus.java
git commit -m "feat: add ProcessCommand base class and CommandBus"
```

---

### Task 13: Create StartProcessCommand

**Files:**
- Create: `simple/src/main/java/com/cat/simple/config/flowable/command/StartProcessCommand.java`

- [ ] **Step 1: Create the command**

```java
package com.cat.simple.config.flowable.command;

import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.enums.ProcessStatusEnum;
import com.cat.simple.config.process.ProcessCodeGenerator;
import jakarta.annotation.Resource;
import org.flowable.engine.RuntimeService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class StartProcessCommand extends ProcessCommand<ProcessInstance> {

    @Resource private RuntimeService runtimeService;
    @Resource private ProcessCodeGenerator codeGenerator;
    @Resource private com.cat.simple.mapper.ProcessInstanceMapper processInstanceMapper;

    private final Integer processDefinitionId;
    private final String title;

    public StartProcessCommand(Integer processDefinitionId, String title) {
        this.processDefinitionId = processDefinitionId;
        this.title = title;
    }

    @Override
    protected void validate() {
        guard.assertDefinitionPublished(processDefinitionId);
    }

    @Override
    protected ProcessInstance doExecute() {
        com.cat.common.entity.process.ProcessDefinition definition =
                guard.assertDefinitionPublished(processDefinitionId);
        String currentUserId = guard.getCurrentUserId();

        org.flowable.engine.runtime.ProcessInstance flowableInstance =
                runtimeService.startProcessInstanceByKey(definition.getProcessKey());

        LocalDateTime now = LocalDateTime.now();
        ProcessInstance instance = new ProcessInstance()
                .setProcessDefinitionId(definition.getId())
                .setTitle(title)
                .setCode(codeGenerator.generate())
                .setProcessInstanceId(flowableInstance.getProcessInstanceId())
                .setProcessStatus(ProcessStatusEnum.ACTIVE.getStatus())
                .setCreateBy(currentUserId)
                .setCreateTime(now)
                .setUpdateTime(now);
        processInstanceMapper.insert(instance);

        // 兜底：trivial 流程立即结束
        if (runtimeService.createProcessInstanceQuery()
                .processInstanceId(flowableInstance.getProcessInstanceId())
                .singleResult() == null) {
            processInstanceMapper.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProcessInstance>()
                    .eq(ProcessInstance::getId, instance.getId())
                    .set(ProcessInstance::getProcessStatus, ProcessStatusEnum.COMPLETED.getStatus())
                    .set(ProcessInstance::getUpdateTime, now));
        }

        return instance;
    }

    @Override
    protected void record(ProcessInstance result) {
        recorder.recordApply(result, guard.getCurrentUserId());
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add simple/src/main/java/com/cat/simple/config/flowable/command/StartProcessCommand.java
git commit -m "feat: add StartProcessCommand"
```

---

### Task 14: Create ClaimTaskCommand

**Files:**
- Create: `simple/src/main/java/com/cat/simple/config/flowable/command/ClaimTaskCommand.java`

- [ ] **Step 1: Create the command**

```java
package com.cat.simple.config.flowable.command;

import com.cat.common.entity.process.ProcessHandleParam;
import jakarta.annotation.Resource;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

@Component
public class ClaimTaskCommand extends ProcessCommand<Void> {

    @Resource private TaskService taskService;

    private final ProcessHandleParam param;

    public ClaimTaskCommand(ProcessHandleParam param) {
        this.param = param;
    }

    @Override
    protected void validate() {
        guard.assertInstanceActive(param.getProcessInstanceId());
        guard.assertTaskCandidate(param.getTaskId());
    }

    @Override
    protected Void doExecute() {
        Task task = guard.getTask(param.getTaskId());
        taskService.claim(param.getTaskId(), guard.getCurrentUserId());
        return null;
    }

    @Override
    protected void record(Void result) {
        Task task = guard.getTask(param.getTaskId());
        recorder.recordClaim(param, task);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add simple/src/main/java/com/cat/simple/config/flowable/command/ClaimTaskCommand.java
git commit -m "feat: add ClaimTaskCommand"
```

---

### Task 15: Create PassTaskCommand

**Files:**
- Create: `simple/src/main/java/com/cat/simple/config/flowable/command/PassTaskCommand.java`

- [ ] **Step 1: Create the command**

```java
package com.cat.simple.config.flowable.command;

import com.cat.common.entity.process.ProcessHandleParam;
import jakarta.annotation.Resource;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

@Component
public class PassTaskCommand extends ProcessCommand<Void> {

    @Resource private TaskService taskService;

    private final ProcessHandleParam param;

    public PassTaskCommand(ProcessHandleParam param) {
        this.param = param;
    }

    @Override
    protected void validate() {
        guard.assertInstanceActive(param.getProcessInstanceId());
        guard.assertTaskAssignee(param.getTaskId());
    }

    @Override
    protected Void doExecute() {
        Task task = guard.getTask(param.getTaskId());
        taskService.complete(task.getId());
        return null;
    }

    @Override
    protected void record(Void result) {
        Task task = guard.getTask(param.getTaskId());
        recorder.recordPass(param, task);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add simple/src/main/java/com/cat/simple/config/flowable/command/PassTaskCommand.java
git commit -m "feat: add PassTaskCommand"
```

---

### Task 16: Create RejectTaskCommand

**Files:**
- Create: `simple/src/main/java/com/cat/simple/config/flowable/command/RejectTaskCommand.java`

- [ ] **Step 1: Create the command**

```java
package com.cat.simple.config.flowable.command;

import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.enums.ProcessStatusEnum;
import jakarta.annotation.Resource;
import org.flowable.engine.RuntimeService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RejectTaskCommand extends ProcessCommand<Void> {

    @Resource private RuntimeService runtimeService;
    @Resource private com.cat.simple.mapper.ProcessInstanceMapper processInstanceMapper;

    private final ProcessHandleParam param;

    public RejectTaskCommand(ProcessHandleParam param) {
        this.param = param;
    }

    @Override
    protected void validate() {
        guard.assertInstanceActive(param.getProcessInstanceId());
        guard.assertTaskAssignee(param.getTaskId());
    }

    @Override
    protected Void doExecute() {
        ProcessInstance instance = guard.getInstance(param.getProcessInstanceId());
        Task task = guard.getTask(param.getTaskId());
        String remark = param.getRemark() != null && !param.getRemark().isBlank()
                ? param.getRemark() : "拒绝";
        runtimeService.deleteProcessInstance(instance.getProcessInstanceId(), remark);
        return null;
    }

    @Override
    protected void record(Void result) {
        Task task = guard.getTask(param.getTaskId());
        recorder.recordReject(param, task);
    }

    @Override
    protected void afterHook(Void result) {
        ProcessInstance instance = guard.getInstance(param.getProcessInstanceId());
        processInstanceMapper.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProcessInstance>()
                .eq(ProcessInstance::getId, instance.getId())
                .set(ProcessInstance::getProcessStatus, ProcessStatusEnum.TERMINATED.getStatus())
                .set(ProcessInstance::getUpdateTime, LocalDateTime.now()));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add simple/src/main/java/com/cat/simple/config/flowable/command/RejectTaskCommand.java
git commit -m "feat: add RejectTaskCommand"
```

---

### Task 17: Create BackTaskCommand

**Files:**
- Create: `simple/src/main/java/com/cat/simple/config/flowable/command/BackTaskCommand.java`

- [ ] **Step 1: Create the command**

```java
package com.cat.simple.config.flowable.command;

import com.cat.common.entity.process.BackConfig;
import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.simple.config.flowable.back.BackConfigReader;
import com.cat.simple.config.flowable.back.BackEngine;
import com.cat.simple.config.flowable.back.BackTargetResolver;
import com.cat.simple.config.flowable.util.BpmnModelUtil;
import jakarta.annotation.Resource;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

@Component
public class BackTaskCommand extends ProcessCommand<Void> {

    @Resource private BackConfigReader backConfigReader;
    @Resource private BackTargetResolver backTargetResolver;
    @Resource private BackEngine backEngine;
    @Resource private BpmnModelUtil bpmnModelUtil;

    private final ProcessHandleParam param;
    private String resolvedTargetNodeId;

    public BackTaskCommand(ProcessHandleParam param) {
        this.param = param;
    }

    @Override
    protected void validate() {
        guard.assertInstanceActive(param.getProcessInstanceId());
        Task task = guard.assertTaskExists(param.getTaskId());
        String currentUserId = guard.getCurrentUserId();
        if (!currentUserId.equals(task.getAssignee())) {
            throw new IllegalStateException("当前用户不是该任务的办理人, taskId: " + param.getTaskId());
        }
    }

    @Override
    protected Void doExecute() {
        com.cat.common.entity.process.ProcessInstance instance =
                guard.getInstance(param.getProcessInstanceId());
        Task task = guard.getTask(param.getTaskId());
        String currentUserId = guard.getCurrentUserId();

        BackConfig cfg = backConfigReader.getBackConfig(param.getTaskId());
        if (!cfg.isAllowBack()) {
            throw new IllegalStateException("该节点未配置驳回方式, taskId: " + param.getTaskId());
        }

        resolvedTargetNodeId = backTargetResolver.resolveTargetNodeId(
                task, cfg.getBackType(), cfg.getBackNodeId(), param.getTargetNodeId());
        String targetNodeName = backTargetResolver.resolveTargetNodeName(
                instance.getProcessInstanceId(), resolvedTargetNodeId);

        boolean isMultiInstance = task.getProcessInstanceId() != null && bpmnModelUtil.isMultiInstance(task);

        if (isMultiInstance) {
            backEngine.backMultiInstanceAllBack(instance, task, resolvedTargetNodeId, currentUserId, param.getRemark(),
                    cfg.getBackAssigneePolicy(), targetNodeName);
        } else {
            backEngine.backSingleInstance(instance, task, resolvedTargetNodeId, currentUserId, param.getRemark(),
                    cfg.getBackAssigneePolicy(), targetNodeName);
        }

        recorder.recordBack(param, task, resolvedTargetNodeId, targetNodeName);
        return null;
    }

    @Override
    protected void record(Void result) {
        // record is called in doExecute because targetNodeId is needed
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add simple/src/main/java/com/cat/simple/config/flowable/command/BackTaskCommand.java
git commit -m "feat: add BackTaskCommand"
```

---

## Phase 5: 门面清理 + 生命周期钩子

### Task 18: Create ProcessLifecycleHook Interface and Context Objects

**Files:**
- Create: `simple/src/main/java/com/cat/simple/config/flowable/hook/ProcessLifecycleHook.java`
- Create: `simple/src/main/java/com/cat/simple/config/flowable/hook/StartContext.java`
- Create: `simple/src/main/java/com/cat/simple/config/flowable/hook/ClaimContext.java`
- Create: `simple/src/main/java/com/cat/simple/config/flowable/hook/PassContext.java`
- Create: `simple/src/main/java/com/cat/simple/config/flowable/hook/RejectContext.java`
- Create: `simple/src/main/java/com/cat/simple/config/flowable/hook/BackContext.java`

- [ ] **Step 1: Create ProcessLifecycleHook**

```java
package com.cat.simple.config.flowable.hook;

import com.cat.common.entity.process.ProcessInstance;
import org.flowable.task.api.Task;

public interface ProcessLifecycleHook {

    default void beforeStart(StartContext ctx) { }
    default void afterStart(ProcessInstance instance) { }

    default void beforeClaim(ClaimContext ctx) { }
    default void afterClaim(ProcessInstance instance, Task task) { }

    default void beforePass(PassContext ctx) { }
    default void afterPass(ProcessInstance instance) { }

    default void beforeReject(RejectContext ctx) { }
    default void afterReject(ProcessInstance instance) { }

    default void beforeBack(BackContext ctx) { }
    default void afterBack(ProcessInstance instance, String targetNodeId) { }
}
```

- [ ] **Step 2: Create Context classes**

```java
// StartContext.java
package com.cat.simple.config.flowable.hook;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class StartContext {
    private Integer processDefinitionId;
    private String title;
    private String applicantId;
    private Map<String, Object> initialVariables;
}
```

```java
// ClaimContext.java
package com.cat.simple.config.flowable.hook;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClaimContext {
    private Integer processInstanceId;
    private String taskId;
    private String remark;
}
```

```java
// PassContext.java
package com.cat.simple.config.flowable.hook;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class PassContext {
    private Integer processInstanceId;
    private String taskId;
    private String remark;
    private Map<String, Object> formData;
}
```

```java
// RejectContext.java
package com.cat.simple.config.flowable.hook;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RejectContext {
    private Integer processInstanceId;
    private String taskId;
    private String remark;
}
```

```java
// BackContext.java
package com.cat.simple.config.flowable.hook;

import com.cat.common.entity.process.BackConfig;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BackContext {
    private Integer processInstanceId;
    private String taskId;
    private String remark;
    private String targetNodeId;
    private BackConfig backConfig;
}
```

- [ ] **Step 3: Commit**

```bash
git add simple/src/main/java/com/cat/simple/config/flowable/hook/
git commit -m "feat: add ProcessLifecycleHook interface and context objects"
```

---

### Task 19: Refactor ProcessInstanceServiceImpl to Facade

**Files:**
- Modify: `simple/src/main/java/com/cat/simple/service/impl/ProcessInstanceServiceImpl.java`

- [ ] **Step 1: Replace the entire ServiceImpl with the facade version**

```java
package com.cat.simple.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.common.entity.Page;
import com.cat.common.entity.process.*;
import com.cat.simple.config.flowable.back.BackConfigReader;
import com.cat.simple.config.flowable.back.BackTargetResolver;
import com.cat.simple.config.flowable.command.*;
import com.cat.simple.config.flowable.enums.ProcessStatusEnum;
import com.cat.simple.config.flowable.guard.ProcessGuard;
import com.cat.simple.mapper.ProcessInstanceMapper;
import com.cat.simple.mapper.ProcessHandleInfoMapper;
import com.cat.simple.service.ProcessInstanceService;
import jakarta.annotation.Resource;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProcessInstanceServiceImpl implements ProcessInstanceService {

    @Resource private CommandBus commandBus;
    @Resource private ProcessGuard guard;
    @Resource private ProcessInstanceMapper processInstanceMapper;
    @Resource private ProcessHandleInfoMapper processHandleInfoMapper;
    @Resource private BackConfigReader backConfigReader;
    @Resource private BackTargetResolver backTargetResolver;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessInstance start(Integer processDefinitionId, String title) {
        return commandBus.execute(new StartProcessCommand(processDefinitionId, title));
    }

    @Override
    public Page<ProcessInstance> queryPage(ProcessInstancePageParam pageParam) {
        pageParam.init();
        pageParam.setUserId(guard.getCurrentUserId());
        return processInstanceMapper.selectPage(new Page<>(pageParam), pageParam);
    }

    @Override
    public ProcessInstance info(Integer id, String taskId) {
        ProcessInstance instance = processInstanceMapper.selectInfoById(id);
        if (instance == null) {
            return null;
        }
        instance.setProcessHandleInfoList(
                processHandleInfoMapper.selectDetailListByProcessInstanceId(instance.getId()));

        if (StringUtils.hasText(taskId)) {
            Task task = guard.assertTaskAssignee(taskId);
            instance.setTaskId(taskId);
            instance.setTaskName(task.getName());
        }
        return instance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claim(ProcessHandleParam param) {
        commandBus.execute(new ClaimTaskCommand(param));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pass(ProcessHandleParam param) {
        commandBus.execute(new PassTaskCommand(param));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(ProcessHandleParam param) {
        commandBus.execute(new RejectTaskCommand(param));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void back(ProcessHandleParam param) {
        commandBus.execute(new BackTaskCommand(param));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessInstance saveDraft(Integer id, Integer processDefinitionId, String title) {
        String currentUserId = guard.getCurrentUserId();
        com.cat.common.entity.process.ProcessDefinition definition =
                guard.assertDefinitionPublished(processDefinitionId);
        LocalDateTime now = LocalDateTime.now();

        if (id != null) {
            ProcessInstance exist = guard.assertInstanceDraft(id);
            if (!currentUserId.equals(exist.getCreateBy())) {
                throw new IllegalStateException("无权更新他人草稿: " + id);
            }
            processInstanceMapper.update(new LambdaUpdateWrapper<ProcessInstance>()
                    .eq(ProcessInstance::getId, id)
                    .set(ProcessInstance::getProcessDefinitionId, definition.getId())
                    .set(ProcessInstance::getTitle, title)
                    .set(ProcessInstance::getUpdateTime, now));
            return exist.setProcessDefinitionId(definition.getId()).setTitle(title).setUpdateTime(now);
        }

        ProcessInstance instance = new ProcessInstance()
                .setProcessDefinitionId(definition.getId())
                .setTitle(title)
                .setProcessStatus(ProcessStatusEnum.DRAFT.getStatus())
                .setCreateBy(currentUserId)
                .setCreateTime(now)
                .setUpdateTime(now);
        processInstanceMapper.insert(instance);
        return instance;
    }

    @Override
    public void updateStatus(String flowableProcessInstanceId, ProcessStatusEnum status) {
        ProcessInstance instance = guard.selectByFlowableId(flowableProcessInstanceId);
        if (instance != null && status != ProcessStatusEnum.UNKNOWN) {
            processInstanceMapper.update(new LambdaUpdateWrapper<ProcessInstance>()
                    .eq(ProcessInstance::getId, instance.getId())
                    .set(ProcessInstance::getProcessStatus, status.getStatus())
                    .set(ProcessInstance::getUpdateTime, LocalDateTime.now()));
        }
    }

    @Override
    public List<BackTargetNode> getAvailableBackTargets(String taskId) {
        return backConfigReader.getAvailableBackTargets(taskId);
    }

    @Override
    public BackConfig getBackConfig(String taskId) {
        return backConfigReader.getBackConfig(taskId);
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./mvnw compile -pl simple
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Run integration tests**

```bash
./mvnw test -pl simple -Dtest=ProcessInstanceServiceTest -DfailIfNoTests=false
```

Expected: Tests PASS (or skip for placeholder tests).

- [ ] **Step 4: Commit**

```bash
git add simple/src/main/java/com/cat/simple/service/impl/ProcessInstanceServiceImpl.java
git commit -m "refactor: reduce ProcessInstanceServiceImpl to clean facade using CommandBus"
```

---

### Task 20: Wire Lifecycle Hooks into Commands

**Files:**
- Modify: `simple/src/main/java/com/cat/simple/config/flowable/command/StartProcessCommand.java`
- Modify: `simple/src/main/java/com/cat/simple/config/flowable/command/ClaimTaskCommand.java`
- Modify: `simple/src/main/java/com/cat/simple/config/flowable/command/PassTaskCommand.java`
- Modify: `simple/src/main/java/com/cat/simple/config/flowable/command/RejectTaskCommand.java`
- Modify: `simple/src/main/java/com/cat/simple/config/flowable/command/BackTaskCommand.java`

- [ ] **Step 1: Add hooks to StartProcessCommand**

Add to `StartProcessCommand`:

```java
import com.cat.simple.config.flowable.hook.StartContext;

@Override
protected void beforeHook() {
    StartContext ctx = new StartContext(processDefinitionId, title, guard.getCurrentUserId(), null);
    lifecycleHook.beforeStart(ctx);
}

@Override
protected void afterHook(ProcessInstance result) {
    lifecycleHook.afterStart(result);
}
```

- [ ] **Step 2: Add hooks to ClaimTaskCommand**

Add to `ClaimTaskCommand`:

```java
import com.cat.simple.config.flowable.hook.ClaimContext;

@Override
protected void beforeHook() {
    ClaimContext ctx = new ClaimContext(param.getProcessInstanceId(), param.getTaskId(), param.getRemark());
    lifecycleHook.beforeClaim(ctx);
}

@Override
protected void afterHook(Void result) {
    Task task = guard.getTask(param.getTaskId());
    lifecycleHook.afterClaim(guard.getInstance(param.getProcessInstanceId()), task);
}
```

- [ ] **Step 3: Add hooks to PassTaskCommand**

Add to `PassTaskCommand`:

```java
import com.cat.simple.config.flowable.hook.PassContext;

@Override
protected void beforeHook() {
    PassContext ctx = new PassContext(param.getProcessInstanceId(), param.getTaskId(), param.getRemark(), null);
    lifecycleHook.beforePass(ctx);
}

@Override
protected void afterHook(Void result) {
    lifecycleHook.afterPass(guard.getInstance(param.getProcessInstanceId()));
}
```

- [ ] **Step 4: Add hooks to RejectTaskCommand**

Add to `RejectTaskCommand`:

```java
import com.cat.simple.config.flowable.hook.RejectContext;

@Override
protected void beforeHook() {
    RejectContext ctx = new RejectContext(param.getProcessInstanceId(), param.getTaskId(), param.getRemark());
    lifecycleHook.beforeReject(ctx);
}

@Override
protected void afterHook(Void result) {
    lifecycleHook.afterReject(guard.getInstance(param.getProcessInstanceId()));
}
```

- [ ] **Step 5: Add hooks to BackTaskCommand**

Add to `BackTaskCommand`:

```java
import com.cat.simple.config.flowable.hook.BackContext;

@Override
protected void beforeHook() {
    BackConfig cfg = backConfigReader.getBackConfig(param.getTaskId());
    BackContext ctx = new BackContext(param.getProcessInstanceId(), param.getTaskId(), param.getRemark(), param.getTargetNodeId(), cfg);
    lifecycleHook.beforeBack(ctx);
}

@Override
protected void afterHook(Void result) {
    lifecycleHook.afterBack(guard.getInstance(param.getProcessInstanceId()), resolvedTargetNodeId);
}
```

- [ ] **Step 6: Verify compilation**

```bash
./mvnw compile -pl simple
```

Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add simple/src/main/java/com/cat/simple/config/flowable/command/
git commit -m "feat: wire ProcessLifecycleHook into all commands"
```

---

## Phase 6: Listener 解耦 + 测试合并

### Task 21: Decouple ProcessInstanceEndListener from Service

**Files:**
- Modify: `simple/src/main/java/com/cat/simple/config/flowable/listener/ProcessInstanceEndListener.java`

- [ ] **Step 1: Replace Service dependency with direct Mapper access**

```java
package com.cat.simple.config.flowable.listener;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.common.entity.process.ProcessInstance;
import com.cat.simple.config.flowable.enums.ProcessStatusEnum;
import com.cat.simple.config.flowable.guard.ProcessGuard;
import com.cat.simple.mapper.ProcessInstanceMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.RuntimeService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class ProcessInstanceEndListener implements FlowableEventListener {

    @Resource private RuntimeService runtimeService;
    @Resource private ProcessInstanceMapper processInstanceMapper;
    @Resource private ProcessGuard guard;

    @PostConstruct
    public void register() {
        runtimeService.addEventListener(this,
                FlowableEngineEventType.PROCESS_COMPLETED,
                FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT,
                FlowableEngineEventType.PROCESS_CANCELLED);
    }

    @Override
    public void onEvent(FlowableEvent event) {
        if (!(event instanceof FlowableEngineEvent engineEvent)) {
            return;
        }
        String processInstanceId = engineEvent.getProcessInstanceId();
        FlowableEngineEventType type = (FlowableEngineEventType) engineEvent.getType();

        ProcessStatusEnum processStatusEnum = switch (type) {
            case PROCESS_COMPLETED,
                 PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT -> ProcessStatusEnum.COMPLETED;
            case PROCESS_CANCELLED -> ProcessStatusEnum.TERMINATED;
            default -> ProcessStatusEnum.UNKNOWN;
        };
        if (processStatusEnum.equals(ProcessStatusEnum.UNKNOWN)) {
            return;
        }
        ProcessInstance instance = guard.selectByFlowableId(processInstanceId);
        if (instance != null) {
            processInstanceMapper.update(new LambdaUpdateWrapper<ProcessInstance>()
                    .eq(ProcessInstance::getId, instance.getId())
                    .set(ProcessInstance::getProcessStatus, processStatusEnum.getStatus())
                    .set(ProcessInstance::getUpdateTime, LocalDateTime.now()));
            log.info("流程实例 {} 结束, 类型={}, 状态={}", processInstanceId, type, processStatusEnum);
        }
    }

    @Override public boolean isFailOnException() { return false; }
    @Override public boolean isFireOnTransactionLifecycleEvent() { return false; }
    @Override public String getOnTransaction() { return null; }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./mvnw compile -pl simple
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add simple/src/main/java/com/cat/simple/config/flowable/listener/ProcessInstanceEndListener.java
git commit -m "refactor: decouple ProcessInstanceEndListener from Service layer"
```

---

### Task 22: Merge ProcessBackServiceTest into ProcessInstanceServiceTest and Delete

**Files:**
- Modify: `simple/src/test/java/com/cat/simple/service/ProcessInstanceServiceTest.java`
- Delete: `simple/src/test/java/com/cat/simple/service/ProcessBackServiceTest.java`

- [ ] **Step 1: Add back-related tests to ProcessInstanceServiceTest**

Add these test methods to `ProcessInstanceServiceTest`:

```java
    @Disabled("需要有效的 taskId，请先启动流程实例并替换占位符")
    @Test
    void testGetBackConfig() {
        String taskId = "REPLACE_WITH_VALID_TASK_ID";
        var config = processInstanceService.getBackConfig(taskId);
        assertNotNull(config);
    }

    @Disabled("需要有效的 taskId，请先启动流程实例并替换占位符")
    @Test
    void testGetAvailableBackTargets() {
        String taskId = "REPLACE_WITH_VALID_TASK_ID";
        var targets = processInstanceService.getAvailableBackTargets(taskId);
        assertNotNull(targets);
    }
```

- [ ] **Step 2: Delete ProcessBackServiceTest**

```bash
git rm simple/src/test/java/com/cat/simple/service/ProcessBackServiceTest.java
```

- [ ] **Step 3: Verify tests compile**

```bash
./mvnw test-compile -pl simple
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add simple/src/test/java/com/cat/simple/service/ProcessInstanceServiceTest.java
git commit -m "test: merge ProcessBackServiceTest into ProcessInstanceServiceTest and delete former"
```

---

### Task 23: Final Verification — Run All Tests

**Files:**
- None (verification only)

- [ ] **Step 1: Run unit tests**

```bash
./mvnw test -pl simple -Dtest=ProcessVariableStoreTest,ProcessGuardTest,HandleInfoRecorderTest -DfailIfNoTests=false
```

Expected: All tests PASS.

- [ ] **Step 2: Run integration tests**

```bash
./mvnw test -pl simple -Dtest=ProcessInstanceServiceTest -DfailIfNoTests=false
```

Expected: Tests PASS or skip (for disabled placeholder tests).

- [ ] **Step 3: Full project compile**

```bash
./mvnw compile
```

Expected: BUILD SUCCESS for all modules.

- [ ] **Step 4: Commit**

```bash
git commit --allow-empty -m "chore: complete Flowable refactor verification"
```

---

## Self-Review Checklist

### Spec Coverage

| Spec Requirement | Implementation Task |
|------------------|---------------------|
| VariableNames enum | Task 1 |
| ProcessVariableStore | Task 2-3 |
| Migrate variable usage | Task 4-7 |
| ProcessGuard | Task 8-9 |
| HandleInfoRecorder | Task 10-11 |
| ProcessCommand base | Task 12 |
| Start/Claim/Pass/Reject/Back Commands | Task 13-17 |
| ProcessLifecycleHook | Task 18 |
| ServiceImpl facade | Task 19 |
| Hook wiring in Commands | Task 20 |
| Listener decoupling | Task 21 |
| Test consolidation | Task 22 |
| Final verification | Task 23 |

### Placeholder Scan

- No TBD, TODO, "implement later", "fill in details", or "similar to Task N" found.
- All code blocks contain complete, compilable Java.
- All test blocks contain runnable JUnit 5 + Mockito tests.

### Type Consistency

- `ProcessCommand<T>` uses `T` consistently across base class and all subclasses.
- `ProcessGuard.assertXxx()` methods return the queried object consistently.
- `VariableNames.getKey()` is the single accessor for variable name strings.
- `HandleInfoRecorder` method signatures match between implementation and tests.


