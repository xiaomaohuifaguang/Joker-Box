# Flowable 流程引擎结构重构设计文档

## 1. 背景与目标

### 1.1 现状问题

`ProcessInstanceServiceImpl` 当前 479 行，混合了以下职责：

- 业务校验（当前用户、实例状态、任务权限）
- Flowable API 直接调用（`runtimeService` / `taskService` / `historyService`）
- 审批轨迹记录（`ProcessHandleInfo` 的构建与保存）
- 流程状态同步（启动后兜底、监听事件更新）
- 驳回逻辑协调（调用 `BackEngine` / `BackTargetResolver` / `BackConfigReader`）

变量读写散落在 4 个位置：
- `ApprovalTaskCreateListener` 直接 `delegateTask.setVariableLocal(...)`
- `BackConfigReader` 通过 `TaskVariableUtil.getLocalVar(...)` 读取
- `BackEngine` 直接 `runtimeService.setVariable(...)` / `removeVariable(...)`
- `ProcessInstanceServiceImpl` 未直接读写变量，但后续扩展动态表单时会涉及

### 1.2 核心目标

1. **ProcessInstanceServiceImpl 干净化**：变为纯门面，只负责参数映射和命令分发
2. **变量统一读写**：所有流程变量通过单一入口读写，消除字符串魔法值
3. **业务扩展点预留**：通过生命周期钩子接口，让后续业务逻辑（通知、外部系统同步、表单校验）以非侵入方式插入
4. **命令模式解耦**：每个审批操作（claim / pass / reject / back / start）封装为独立命令，职责单一、可独立测试

---

## 2. 架构设计

### 2.1 核心组件图

```
┌─────────────────────────────────────────────────────────────┐
│                  ProcessInstanceServiceImpl                  │
│                     (Facade / 纯门面)                        │
│  职责：参数校验 → 构造 Command → 交给 CommandBus 执行         │
└──────────────────────┬──────────────────────────────────────┘
                       │ inject
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                        CommandBus                            │
│  职责：Command 分发、事务控制、异常转换                        │
└──────────────────────┬──────────────────────────────────────┘
                       │ execute(command)
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                    ProcessCommand<T>                         │
│                     (抽象基类)                               │
│  模板方法：                                                  │
│    1. validate()  ──▶ ProcessGuard 通用校验                  │
│    2. doExecute() ──▶ 子类实现业务逻辑                       │
│    3. record()    ──▶ HandleInfoRecorder 记录轨迹            │
│    4. hook()      ──▶ ProcessLifecycleHook 扩展钩子          │
└──────┬───────────────────────────────────────────────────────┘
       │ extends
       ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│  Start   │ │  Claim   │ │   Pass   │ │  Reject  │ │   Back   │
│ Command  │ │ Command  │ │ Command  │ │ Command  │ │ Command  │
└──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘
```

### 2.2 辅助组件

```
┌────────────────────────┐    ┌────────────────────────┐
│   ProcessVariableStore │    │     ProcessGuard       │
│   统一变量读写层        │    │   通用校验逻辑          │
│                        │    │                        │
│  - setProcessVar()     │    │  - getCurrentUserId()  │
│  - getProcessVar()     │    │  - assertInstanceActive│
│  - setTaskLocalVar()   │    │  - assertTaskAssignee  │
│  - getTaskLocalVar()   │    │  - assertTaskCandidate │
│  - removeProcessVar()  │    │  - assertNotNull(...)  │
└────────────────────────┘    └────────────────────────┘

┌────────────────────────┐    ┌────────────────────────┐
│   HandleInfoRecorder   │    │  ProcessLifecycleHook  │
│   审批轨迹记录器        │    │   业务扩展钩子接口      │
│                        │    │                        │
│  - recordApply()       │    │  - beforeStart()       │
│  - recordClaim()       │    │  - afterStart()        │
│  - recordPass()        │    │  - beforePass()        │
│  - recordReject()      │    │  - afterPass()         │
│  - recordBack()        │    │  - beforeClaim()       │
│  - recordCustom()      │    │  - afterClaim()        │
└────────────────────────┘    │  - ...                 │
                              └────────────────────────┘
```

---

## 3. 组件详细设计

### 3.1 Command 体系

#### 3.1.1 抽象基类 ProcessCommand

```java
public abstract class ProcessCommand<T> {

    @Resource protected ProcessGuard guard;
    @Resource protected HandleInfoRecorder recorder;
    @Resource protected ProcessLifecycleHook lifecycleHook;
    @Resource protected ProcessVariableStore variableStore;

    /**
     * 模板方法：定义执行骨架
     */
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

**设计说明：**
- `validate()` 由子类实现，但鼓励复用 `ProcessGuard` 的方法
- `beforeHook()` / `afterHook()` 默认空实现，子类可覆盖或依赖 `ProcessLifecycleHook`
- 返回值类型 `T` 用泛型：`StartProcessCommand` 返回 `ProcessInstance`，`PassTaskCommand` 返回 `Void`

#### 3.1.2 具体命令示例

**PassTaskCommand：**
```java
@Component
public class PassTaskCommand extends ProcessCommand<Void> {

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
    protected void beforeHook() {
        lifecycleHook.beforePass(buildPassContext());
    }

    @Override
    protected Void doExecute() {
        Task task = guard.getTask(param.getTaskId());
        taskService.complete(task.getId());
        return null;
    }

    @Override
    protected void afterHook(Void result) {
        lifecycleHook.afterPass(guard.getInstance(param.getProcessInstanceId()));
    }

    @Override
    protected void record(Void result) {
        Task task = guard.getTask(param.getTaskId());
        recorder.recordPass(param, task);
    }
}
```

**设计说明：**
- 命令类作为 Spring Bean，通过构造函数注入参数（非字段注入）
- 每个命令只负责一种操作，内部不再分支判断
- `ProcessGuard` 同时提供"断言"和"查询"能力，避免重复查询

#### 3.1.3 CommandBus

```java
@Component
public class CommandBus {

    @Resource private ApplicationContext applicationContext;

    /**
     * 执行命令。Command 必须是 Spring Bean（原型作用域）。
     */
    @Transactional(rollbackFor = Exception.class)
    public <T> T execute(ProcessCommand<T> command) {
        return command.execute();
    }
}
```

**设计说明：**
- `CommandBus` 统一管理事务边界
- 命令类使用 `@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)`，每次执行时通过 `applicationContext.getBean()` 获取新实例，避免状态共享
- 如果命令类不依赖 Spring Bean，也可以直接 `new` 创建后交给 CommandBus

---

### 3.2 ProcessVariableStore（统一变量读写层）

```java
@Component
public class ProcessVariableStore {

    @Resource private RuntimeService runtimeService;
    @Resource private TaskService taskService;

    // ========== 流程实例变量 ==========
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

    // ========== 任务局部变量 ==========
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

**设计说明：**
- 所有变量名统一收敛到 `VariableNames` 枚举，消除字符串魔法值
- 提供类型安全的泛型 API，避免强制转换
- 同时支持流程实例级变量和任务局部变量
- 后续动态表单数据也通过此层存储：`store.set(processInstanceId, VariableNames.FORM_DATA, formMap)`

#### 3.2.1 VariableNames 枚举

```java
@AllArgsConstructor
@Getter
public enum VariableNames {
    // BPMN 扩展元素映射（对应现有 ProcessConstants）
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

    // 运行时变量
    FORM_DATA("formData"),

    // 回退相关
    BACK_ASSIGNEES_PREFIX("__back_assignees_");

    private final String key;
}
```

**设计说明：**
- 将现有 `ProcessConstants` 中的字符串常量和 `ExtensionElementEnum` 的职责合并
- `ExtensionElementEnum` 保留用于 BPMN 解析阶段描述，`VariableNames` 用于运行时变量读写
- `BACK_ASSIGNEES_PREFIX` 保留前缀语义，使用时拼接节点 ID

---

### 3.3 ProcessGuard（通用校验与查询）

```java
@Component
public class ProcessGuard {

    @Resource private ProcessInstanceMapper processInstanceMapper;
    @Resource private ProcessDefinitionMapper processDefinitionMapper;
    @Resource private TaskService taskService;
    @Resource private RuntimeService runtimeService;
    @Resource private HistoryService historyService;

    // ========== 用户安全 ==========
    public String getCurrentUserId() {
        return Optional.ofNullable(SecurityUtils.getLoginUser())
                .map(LoginUser::getUserId)
                .orElseThrow(() -> new IllegalStateException("当前未登录"));
    }

    // ========== 流程实例断言 ==========
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

    // ========== 任务断言 ==========
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

    // ========== 流程定义断言 ==========
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

    // ========== 查询方法（供 Command 使用） ==========
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

**设计说明：**
- 所有"查询 + 校验"的逻辑集中在此，ServiceImpl 和 Command 都不再直接写 `if (xxx == null) throw ...`
- 断言方法返回查询结果，避免 Command 里二次查询
- 用户安全信息统一从此获取，后续如果要支持代理审批、系统操作人，只需改这一处

---

### 3.4 HandleInfoRecorder（审批轨迹记录器）

```java
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

**设计说明：**
- 消除 ServiceImpl 中大量重复的 `saveHandleInfo(new ProcessHandleInfo().setXxx(...).setYyy(...))`
- 每个审批动作一个方法，语义清晰
- `buildBase` 抽取公共字段，`resolveRound` 抽取轮次计算
- `insert` 方法统一处理默认值（handleTime、round）

---

### 3.5 ProcessLifecycleHook（业务扩展钩子）

```java
/**
 * 流程生命周期钩子接口。
 * 业务方实现此接口并注册为 Spring Bean，即可在关键节点插入自定义逻辑。
 * 所有方法都有默认空实现，业务方只需覆盖关心的方法。
 */
public interface ProcessLifecycleHook {

    // ---- 启动 ----
    default void beforeStart(StartContext ctx) { }
    default void afterStart(ProcessInstance instance) { }

    // ---- 认领 ----
    default void beforeClaim(ClaimContext ctx) { }
    default void afterClaim(ProcessInstance instance, Task task) { }

    // ---- 通过 ----
    default void beforePass(PassContext ctx) { }
    default void afterPass(ProcessInstance instance) { }

    // ---- 拒绝 ----
    default void beforeReject(RejectContext ctx) { }
    default void afterReject(ProcessInstance instance) { }

    // ---- 驳回 ----
    default void beforeBack(BackContext ctx) { }
    default void afterBack(ProcessInstance instance, String targetNodeId) { }
}
```

**Context 对象设计：**

```java
@Data
@AllArgsConstructor
public class StartContext {
    private Integer processDefinitionId;
    private String title;
    private String applicantId;
    // 允许 Hook 修改标题或注入初始变量
    private Map<String, Object> initialVariables;
}

@Data
@AllArgsConstructor
public class PassContext {
    private Integer processInstanceId;
    private String taskId;
    private String remark;
    // 动态表单数据，Hook 可校验
    private Map<String, Object> formData;
}

@Data
@AllArgsConstructor
public class ClaimContext {
    private Integer processInstanceId;
    private String taskId;
    private String remark;
}

@Data
@AllArgsConstructor
public class RejectContext {
    private Integer processInstanceId;
    private String taskId;
    private String remark;
}

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

**设计说明：**
- 使用 Java 8 的 `default` 方法，业务方只需实现关心的钩子
- Context 对象携带足够的上下文信息，同时允许 Hook 修改部分数据（如 `StartContext.title`）
- `beforeXxx` 抛异常可阻止流程继续，实现前置校验
- `afterXxx` 适合发送通知、同步外部系统等后置操作
- 多个 Hook 实现时通过 `@Order` 控制执行顺序

---

### 3.6 状态同步策略调整

现有 `ProcessInstanceEndListener` 直接调用 `processInstanceService.updateStatus()`，重构后调整为：

```java
@Component
public class ProcessInstanceEndListener implements FlowableEventListener {
    @Resource private ProcessInstanceMapper processInstanceMapper;
    @Resource private ProcessGuard guard; // 复用查询能力

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
        }
    }
}
```

**设计说明：**
- Listener 不再依赖 Service 层，直接操作 Mapper，避免循环依赖
- 复用 `ProcessGuard` 的查询方法

---

## 4. 文件结构

### 4.1 新增文件

```
simple/src/main/java/com/cat/simple/config/flowable/
├── command/
│   ├── ProcessCommand.java              # 抽象基类
│   ├── CommandBus.java                  # 命令分发器
│   ├── StartProcessCommand.java
│   ├── ClaimTaskCommand.java
│   ├── PassTaskCommand.java
│   ├── RejectTaskCommand.java
│   └── BackTaskCommand.java
├── variable/
│   ├── ProcessVariableStore.java        # 统一变量读写
│   └── VariableNames.java               # 变量名枚举
├── guard/
│   └── ProcessGuard.java                # 通用校验与查询
├── recorder/
│   └── HandleInfoRecorder.java          # 审批轨迹记录
├── hook/
│   ├── ProcessLifecycleHook.java        # 扩展钩子接口
│   ├── StartContext.java                # 启动上下文
│   ├── PassContext.java                 # 通过上下文
│   ├── ClaimContext.java                # 认领上下文
│   ├── RejectContext.java               # 拒绝上下文
│   └── BackContext.java                 # 驳回上下文
└── util/
    ├── BpmnModelUtil.java               # 保留（不变）
    └── TaskVariableUtil.java            # 废弃（功能合并到 ProcessVariableStore）
```

### 4.2 修改文件

```
simple/src/main/java/com/cat/simple/service/impl/
└── ProcessInstanceServiceImpl.java      # 大幅精简，变门面

simple/src/main/java/com/cat/simple/config/flowable/
├── listener/
│   ├── ApprovalTaskCreateListener.java  # 改用 ProcessVariableStore
│   └── ProcessInstanceEndListener.java  # 移除 Service 依赖
├── back/
│   ├── BackConfigReader.java            # 改用 ProcessVariableStore
│   ├── BackEngine.java                  # 改用 ProcessVariableStore
│   └── BackTargetResolver.java          # 保持不变
├── constant/
│   └── ProcessConstants.java            # 保留，但改为引用 VariableNames
└── approval/
    └── ApprovalContext.java             # 保持不变
```

### 4.3 废弃文件

```
simple/src/main/java/com/cat/simple/config/flowable/util/TaskVariableUtil.java
# 功能合并到 ProcessVariableStore，删除此文件
```

---

## 5. ProcessInstanceServiceImpl 重构后预览

```java
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
        return commandBus.execute(
                new StartProcessCommand(processDefinitionId, title));
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
    public ProcessInstance saveDraft(Integer id, Integer processDefinitionId, String title) {
        // 草稿逻辑不涉及 Flowable 引擎，保持现有实现不变
        // 如需后续提取为 Command，可新增 SaveDraftCommand
        String currentUserId = guard.getCurrentUserId();
        ProcessDefinition definition = guard.assertDefinitionPublished(processDefinitionId);
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

**行数预估：**从 479 行 → 约 90 行。

---

## 6. 数据流示例

### 6.1 审批通过（pass）

```
Controller 调用 pass(param)
    │
    ▼
ProcessInstanceServiceImpl.pass(param)
    │
    ▼
CommandBus.execute(new PassTaskCommand(param))
    │
    ▼
PassTaskCommand.execute() [模板方法]
    │
    ├── validate()
    │   └── ProcessGuard.assertInstanceActive(pid)
    │   └── ProcessGuard.assertTaskAssignee(taskId)
    │
    ├── beforeHook()
    │   └── ProcessLifecycleHook.beforePass(ctx)
    │       └── [业务扩展] 校验表单数据
    │
    ├── doExecute()
    │   └── taskService.complete(taskId)
    │
    ├── afterHook()
    │   └── ProcessLifecycleHook.afterPass(instance)
    │       └── [业务扩展] 发送审批通过通知
    │
    └── record()
        └── HandleInfoRecorder.recordPass(param, task)
            └── 插入 process_handle_info 记录
```

### 6.2 启动流程（start）

```
Controller 调用 start(definitionId, title)
    │
    ▼
ProcessInstanceServiceImpl.start(definitionId, title)
    │
    ▼
CommandBus.execute(new StartProcessCommand(definitionId, title))
    │
    ▼
StartProcessCommand.execute()
    │
    ├── validate()
    │   └── ProcessGuard.assertDefinitionPublished(definitionId)
    │
    ├── beforeHook()
    │   └── ProcessLifecycleHook.beforeStart(ctx)
    │       └── [业务扩展] 修改标题格式 / 注入业务变量
    │
    ├── doExecute()
    │   ├── runtimeService.startProcessInstanceByKey(processKey)
    │   ├── processInstanceMapper.insert(instance)
    │   └── [兜底] 同步更新已完成状态
    │
    ├── afterHook()
    │   └── ProcessLifecycleHook.afterStart(instance)
    │       └── [业务扩展] 发送发起成功通知
    │
    └── record()
        └── HandleInfoRecorder.recordApply(instance, userId)
```

---

## 7. 迁移策略

### 7.1 迁移顺序

1. **Phase 1：基础设施**（无业务影响）
   - 创建 `VariableNames` 枚举
   - 创建 `ProcessVariableStore`
   - 修改 `ApprovalTaskCreateListener` 和 `BackConfigReader` 使用新变量层
   - 删除 `TaskVariableUtil`

2. **Phase 2：校验层**（无业务影响）
   - 创建 `ProcessGuard`
   - 逐步将 ServiceImpl 中的校验逻辑迁移到 Guard

3. **Phase 3：记录层**（无业务影响）
   - 创建 `HandleInfoRecorder`
   - 将 ServiceImpl 中的 HandleInfo 记录迁移到 Recorder

4. **Phase 4：命令层**（核心重构）
   - 创建 `ProcessCommand` 抽象基类
   - 逐个创建具体 Command（Start / Claim / Pass / Reject / Back）
   - 每创建一个 Command，就将 ServiceImpl 对应方法替换为 CommandBus 调用

5. **Phase 5：门面清理 + 钩子层**
   - 清理 ServiceImpl，移除所有私有方法
   - 创建 `ProcessLifecycleHook` 接口和 Context 对象
   - 在 Command 模板方法中接入 Hook

6. **Phase 6：Listener 解耦**
   - 修改 `ProcessInstanceEndListener` 移除 Service 依赖

### 7.2 回滚预案

- 每个 Phase 独立可回滚
- Phase 1-3 是"新增 + 引用"，不删除旧代码，可立即回滚
- Phase 4 每个 Command 替换一个方法，如果出问题可单独回滚单个方法
- 建议每个 Phase 结束后运行测试确认

---

## 8. 测试策略

### 8.1 单元测试

- **ProcessGuard 测试**：mock Mapper 和 Flowable Service，测试各种校验场景
- **HandleInfoRecorder 测试**：mock Mapper，验证字段映射正确
- **ProcessVariableStore 测试**：mock RuntimeService / TaskService，验证变量名正确、类型转换正确
- **Command 测试**：mock 所有依赖，独立测试每个 Command 的 `doExecute()` 逻辑

### 8.2 集成测试

- 保留现有 `ProcessInstanceServiceTest`，将 `ProcessBackServiceTest` 中的驳回相关测试用例合并到其中，然后删除 `ProcessBackServiceTest`
- 重构后这些测试仍然通过，验证行为一致性
- 新增 `ProcessLifecycleHookTest`：验证 Hook 在正确时机被调用

---

## 9. 后续扩展预留

### 9.1 动态表单

- 表单数据以 `Map<String, Object>` 存储在流程变量中
- `ProcessVariableStore.set(processInstanceId, VariableNames.FORM_DATA, formMap)`
- Hook 中通过 `PassContext.getFormData()` 获取并校验

### 9.2 审批变量

- 新增 `VariableNames.APPROVAL_VARIABLES`
- 在 `StartContext` / `PassContext` 中预留 `variables` 字段
- 业务方通过 Hook 注入自定义变量

### 9.3 转办 / 抄送 / 撤回

- 新增 `TransferTaskCommand` / `CarbonCopyCommand` / `WithdrawCommand`
- 继承 `ProcessCommand`，遵循相同模板
- 新增对应 Hook 方法：`beforeTransfer` / `afterTransfer` 等