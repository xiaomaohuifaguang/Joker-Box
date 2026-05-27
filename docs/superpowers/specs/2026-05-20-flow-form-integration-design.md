# 审批流动态表单绑定设计方案

## 一、背景与目标

### 1.1 现状
- 已有基于 Flowable 的审批流系统，支持流程定义、实例管理、任务处理、会签/或签、驳回等
- 已有完善的动态表单系统，支持字段分组、15+ 种字段类型、联动规则、版本管理
- **目前流程与表单完全独立，无关联**

### 1.2 目标
实现流程与动态表单深度集成，支持：
1. 节点级表单绑定（不同节点可绑定不同表单）
2. 表单字段作为网关流转条件
3. 节点控制表单字段的显隐/可编辑/必填
4. 表单字段作为审批流列表查询条件
5. 支持并行、回退等复杂操作的表单数据一致性

---

## 二、总体架构

### 2.1 核心原则
- **表单实例独立存储**：每个节点的表单数据独立保存，与 Flowable 变量解耦
- **按需变量同步**：仅网关条件需要的字段同步到 Flowable 变量
- **分层条件路由**：简单条件用原生表达式，复杂条件用 Spring Bean，极端场景用配置化路由
- **版本锁定**：流程实例启动后锁定当时的表单版本

### 2.2 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      BPMN 流程定义                           │
│  排他网关条件表达式（三层混合使用）：                          │
│    ${amount > 10000}                                         │
│    ${deptService.isApproverInFinance(execution, 'node_1')}   │
│    ${amount > 10000 && budgetApi.checkAvailable(execution)}  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    流程引擎（Flowable）                       │
│  - 变量表：只存关键字段（amount, level 等）                   │
│  - 网关判断：JUEL 表达式引擎                                  │
│  - 任务调度：并行/串行/回退                                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   表单数据存储（自建）                         │
│  cat_form_instance        -- 表单实例（关联流程实例+节点）     │
│  cat_form_field_value     -- 字段值                          │
│  cat_form_snapshot        -- 表单快照（每次提交保留）         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   配置存储（自建）                             │
│  cat_process_form_binding       -- 流程-表单绑定             │
│  cat_process_node_form_config   -- 节点表单配置              │
│  cat_process_form_field_mapping -- 字段→变量映射             │
└─────────────────────────────────────────────────────────────┘
```

---

## 三、流程定义版本管理

### 3.1 问题

当前 `cat_process_definition_bytearray` 与 `cat_process_definition` 是 1:1 关系（id 相同），每次保存直接覆盖，历史 BPMN XML 丢失。而表单已有完善的版本管理（DRAFT → 版本号快照），流程定义缺少对应能力，导致：

- 无法回看历史版本
- 无法回滚到上一版本
- 后续绑定表单后，无法追溯"当时绑的是哪套配置"

### 3.2 方案：与表单版本管理对齐

**核心改动：给 `cat_process_definition_bytearray` 加 `version` 列，复用表单的 DRAFT + 版本号快照模式。**

#### 改造后的表结构

```sql
-- 改造前
cat_process_definition_bytearray
  id      (INT, PK, = process_definition.id)   -- 1:1 绑定，无版本
  xml     (LONGBLOB)
  raw_data (JSON)

-- 改造后
cat_process_definition_bytearray
  id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
  process_definition_id   INT NOT NULL COMMENT '关联流程定义ID（逻辑关联，无外键）',
  version                 VARCHAR(32) NOT NULL COMMENT 'DRAFT / 1 / 2 / 3 ...',
  xml                     LONGBLOB NOT NULL,
  raw_data                JSON COMMENT 'logicFlow data',
  create_by               VARCHAR(64),
  create_time             DATETIME,
  UNIQUE KEY uk_def_version (process_definition_id, version)
```

#### 版本生命周期

```
创建流程
  │
  ▼
主表：version="DRAFT", status="0"
bytearray：process_definition_id=1, version="DRAFT"  ← 编辑态，可反复覆盖保存
  │
  │  点击发布
  ▼
主表：version="1", status="1"
bytearray：
  - process_definition_id=1, version="1"   ← 发布快照，不可修改
  - process_definition_id=1, version="DRAFT" ← 删除
  │
  │  修改后再次发布
  ▼
主表：version="2", status="1"
bytearray：
  - process_definition_id=1, version="1"   ← 历史快照
  - process_definition_id=1, version="2"   ← 最新发布
  - process_definition_id=1, version="DRAFT" ← 删除
  │
  │  停用
  ▼
主表：version="2", status="-1"
bytearray：
  - process_definition_id=1, version="1"   ← 历史快照
  - process_definition_id=1, version="2"   ← 最后发布
  - process_definition_id=1, version="DRAFT" ← 从 version=2 复制回来，可继续编辑
```

#### 各操作行为

| 操作 | 改造后的行为 |
|---|---|
| **新增** | 主表插入，bytearray 插入 version="DRAFT" |
| **保存** | 覆盖 version="DRAFT" 的 bytearray |
| **发布** | 复制 DRAFT → 新版本号 → 部署到 Flowable → 删除 DRAFT |
| **停用** | 复制最新版本回 DRAFT → 改状态为 -1 |
| **查看详情** | 读指定 version 的 bytearray（默认 DRAFT） |
| **版本列表** | 查询该 process_definition_id 的所有版本 |
| **回滚到V1** | 复制 version="1" 的数据回 DRAFT → 重新发布 |

#### 与表单操作代码对比

两者逻辑一致：

```java
// 表单发布（现有）
dynamicFormFieldMapper.copyVersion(formId, "DRAFT", newVersion);
dynamicFormFieldMapper.deletePhysicsByFormIdAndVersion(formId, "DRAFT");
dynamicForm.setVersion(newVersion);

// 流程发布（改造后）
bytearrayMapper.copyVersion(processDefinitionId, "DRAFT", newVersion);
bytearrayMapper.deletePhysicsByDefAndVersion(processDefinitionId, "DRAFT");
processDefinition.setVersion(newVersion);
```

#### 与 Flowable 版本的映射

```
cat_process_definition_bytearray  version="1"  ↔  ACT_RE_PROCDEF  VERSION_ = 1
cat_process_definition_bytearray  version="2"  ↔  ACT_RE_PROCDEF  VERSION_ = 2
```

主表的 `version` 字段与 Flowable 版本号一致，bytearray 的版本号也与之对应。

#### 实体类改动

```java
// ProcessDefinitionBytearray 改造后
@Data
@TableName("cat_process_definition_bytearray")
public class ProcessDefinitionBytearray implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer processDefinitionId;  // 逻辑关联，无外键
    private String version;               // "DRAFT" / "1" / "2"
    private byte[] xml;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> rawData;
    private String createBy;
    private LocalDateTime createTime;
}
```

#### 数据迁移

```sql
-- 1. 备份
CREATE TABLE cat_process_definition_bytearray_bak AS
  SELECT * FROM cat_process_definition_bytearray;

-- 2. 新建表（原表结构不支持直接 ALTER 加唯一索引，建议重建）
CREATE TABLE cat_process_definition_bytearray_new (
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    process_definition_id   INT NOT NULL,
    version                 VARCHAR(32) NOT NULL,
    xml                     LONGBLOB NOT NULL,
    raw_data                JSON,
    create_by               VARCHAR(64),
    create_time             DATETIME,
    UNIQUE KEY uk_def_version (process_definition_id, version)
);

-- 3. 迁移数据
-- 已发布的流程：version 取 process_definition.version
-- 草稿流程：version = 'DRAFT'
INSERT INTO cat_process_definition_bytearray_new (process_definition_id, version, xml, raw_data, create_by, create_time)
SELECT id,
       CASE WHEN status = '0' THEN 'DRAFT'
            WHEN status IN ('1', '-1') THEN version
       END,
       xml, raw_data, create_by, NOW()
FROM cat_process_definition_bytearray;

-- 4. 替换原表
DROP TABLE cat_process_definition_bytearray;
RENAME TABLE cat_process_definition_bytearray_new TO cat_process_definition_bytearray;
```

---

## 四、数据模型设计

> **版本管理统一原则**：所有跟随流程定义的配置表（流程-表单绑定、节点字段权限）均增加 `version` 字段，与 `cat_process_definition_bytearray` 保持一致的版本生命周期：`DRAFT`（编辑态）→ 发布时复制到版本号 → 停用/回滚时从版本号复制回 `DRAFT`。

### 4.1 流程定义-表单绑定表（所有表均无外键，仅逻辑关联）

```sql
CREATE TABLE cat_process_definition_form (
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    process_definition_id   INT NOT NULL COMMENT '自建流程定义ID',
    version                 VARCHAR(32) NOT NULL COMMENT 'DRAFT / 1 / 2 / 3 ...',
    form_id                 VARCHAR(64) NOT NULL COMMENT '表单ID',
    bind_type               VARCHAR(32) NOT NULL COMMENT 'GLOBAL-全局默认 / NODE-节点绑定',
    node_id                 VARCHAR(64) COMMENT 'BPMN节点ID，GLOBAL时为空',
    inherit_main_form       VARCHAR(1) DEFAULT '0' COMMENT '节点是否继承主表单字段',
    create_by               VARCHAR(64),
    create_time             DATETIME,
    UNIQUE KEY uk_def_version_node (process_definition_id, version, node_id)
);
```

### 4.2 节点字段权限表

```sql
CREATE TABLE cat_process_node_field_permission (
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    process_definition_id   INT NOT NULL,
    version                 VARCHAR(32) NOT NULL COMMENT 'DRAFT / 1 / 2 / 3 ...',
    node_id                 VARCHAR(64) NOT NULL COMMENT 'BPMN节点ID',
    field_key               VARCHAR(128) NOT NULL COMMENT '字段标识（对应表单 field.key）',
    permission              VARCHAR(32) NOT NULL COMMENT 'VISIBLE / READONLY / HIDDEN / EDITABLE / REQUIRED',
    create_by               VARCHAR(64),
    create_time             DATETIME,
    UNIQUE KEY uk_def_version_node_field (process_definition_id, version, node_id, field_key)
);
```

### 版本生命周期（新增配置表）

```
创建流程
  │
  ▼
bytearray：version="DRAFT"
node_form：version="DRAFT"  （空配置）
node_permission：version="DRAFT" （空配置）
  │
  │  点击发布
  ▼
bytearray：复制 DRAFT → "1"，删除 DRAFT
node_form：复制 DRAFT → "1"，删除 DRAFT
node_permission：复制 DRAFT → "1"，删除 DRAFT
  │
  │  停用
  ▼
bytearray：复制 "1" → DRAFT
node_form：复制 "1" → DRAFT
node_permission：复制 "1" → DRAFT
  │
  │  回滚到 V1
  ▼
bytearray：复制 "1" → DRAFT
node_form：复制 "1" → DRAFT
node_permission：复制 "1" → DRAFT
```

### 4.3 字段→变量映射表

```sql
CREATE TABLE cat_process_form_field_mapping (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    process_definition_id INT NOT NULL,
    form_id             VARCHAR(64) NOT NULL,
    field_id            VARCHAR(64) NOT NULL COMMENT '表单字段ID',
    variable_name       VARCHAR(64) NOT NULL COMMENT 'Flowable变量名',
    variable_type       VARCHAR(32) DEFAULT 'STRING' COMMENT 'STRING/INTEGER/DOUBLE/DATE/BOOLEAN',
    sync_timing         VARCHAR(32) DEFAULT 'SUBMIT' COMMENT 'START/SUBMIT/ALWAYS',
    create_by           VARCHAR(64) NOT NULL,
    create_time         DATETIME NOT NULL,
    update_time         DATETIME NOT NULL,
    deleted             VARCHAR(1) DEFAULT '0',
    UNIQUE KEY uk_mapping (process_definition_id, form_id, field_id)
);
```

### 4.4 表单实例表（扩展）

在现有 `cat_dynamic_form_instance` 基础上扩展：

```sql
ALTER TABLE cat_dynamic_form_instance ADD COLUMN (
    process_instance_id INT COMMENT '自建流程实例ID',
    process_definition_id INT COMMENT '自建流程定义ID',
    node_key            VARCHAR(64) COMMENT 'BPMN节点ID',
    node_round          INT DEFAULT 1 COMMENT '节点轮次（用于回退后重新激活）',
    snapshot_of         VARCHAR(64) COMMENT '快照来源表单实例ID'
);
```

### 4.5 表单快照表

```sql
CREATE TABLE cat_form_snapshot (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    form_instance_id    VARCHAR(64) NOT NULL COMMENT '原表单实例ID',
    process_instance_id INT NOT NULL,
    node_key            VARCHAR(64) NOT NULL,
    node_round          INT NOT NULL,
    snapshot_data       JSON NOT NULL COMMENT '表单数据完整快照',
    create_by           VARCHAR(64) NOT NULL,
    create_time         DATETIME NOT NULL,
    INDEX idx_instance (process_instance_id, node_key, node_round)
);
```

---

## 五、三层条件路由体系

### 5.1 第一层：原生 JUEL 表达式（80% 场景）

BPMN 写法：
```xml
<conditionExpression xsi:type="bpmn:tFormalExpression">
    ${amount > 10000 &amp;&amp; level == 'high'}
</conditionExpression>
```

要求：
- `amount`、`level` 等字段已同步到 Flowable 变量表
- 纯内存计算，性能最优

### 5.2 第二层：Spring Bean 方法调用（15% 场景）

BPMN 写法：
```xml
<conditionExpression xsi:type="bpmn:tFormalExpression">
    ${deptService.isApproverInFinance(execution, 'manager_review')}
</conditionExpression>
```

实现示例：
```java
@Component("deptService")
public class DeptRouteService {
    
    public boolean isApproverInFinance(DelegateExecution execution, String nodeKey) {
        // 1. 通过 execution 获取流程实例ID
        String processInstanceId = execution.getProcessInstanceId();
        
        // 2. 查历史任务获取指定节点的处理人
        HistoricTaskInstance task = historyService
            .createHistoricTaskInstanceQuery()
            .processInstanceId(processInstanceId)
            .taskDefinitionKey(nodeKey)
            .finished()
            .orderByHistoricTaskInstanceEndTime()
            .desc()
            .listPage(0, 1)
            .stream().findFirst().orElse(null);
        
        if (task == null || task.getAssignee() == null) {
            return false;
        }
        
        // 3. 查用户部门
        User user = userMapper.selectById(task.getAssignee());
        return user != null && "财务部".equals(user.getDeptName());
    }
}
```

### 5.3 第三层：统一路由服务（5% 场景）

BPMN 写法：
```xml
<conditionExpression xsi:type="bpmn:tFormalExpression">
    ${flowRouter.execute(execution, 'flow_pass')}
</conditionExpression>
```

适用场景：
- 条件需要运营人员随时调整
- 条件涉及多数据源组合判断
- A/B 测试等动态策略

实现要点：
- 条件配置存数据库，支持版本化
- 必须加 Redis 缓存（防止每次查库）
- 必须记录审计日志（因为条件不透明）

### 5.4 混合使用

三层可以混合在同一条表达式中：
```xml
${(amount > 10000 || urgent == true) 
   &amp;&amp; deptService.isApproverInFinance(execution, 'manager_review')
   &amp;&amp; budgetApi.checkAvailable(execution, amount)}
```

JUEL 的短路求值机制确保性能：如果 `amount <= 10000` 且 `urgent != true`，后面的 Bean 方法不会执行。

---

## 六、数据流转机制

### 6.1 表单实例生命周期

```
流程启动
  │
  ▼
创建全局表单实例（cat_form_instance，node_key = '_start'）
  │
  ▼
用户填写并提交
  │
  ├── 保存表单字段值（cat_form_field_value）
  ├── 同步映射字段到 Flowable 变量（execution.setVariable）
  ├── 生成表单快照（cat_form_snapshot）
  └── 触发流程流转（taskService.complete）
        │
        ▼
    到达节点 A
        │
        ▼
    创建节点 A 的表单实例
        │
        ├── 根据 data_inherit_from 配置继承上游数据
        ├── 应用 node_form_config 的字段权限（显隐/可编辑/必填）
        └── 用户看到带有权限控制的表单
```

### 6.2 数据继承策略

| 策略 | 说明 | 配置值 |
|---|---|---|
| 继承全部 | 复制上游表单实例的所有字段值 | `INHERIT_ALL` |
| 继承指定 | 只复制指定的字段 | `INHERIT_SELECTED` |
| 全新开始 | 不继承，使用表单模板默认值 | `FRESH_START` |

继承逻辑：
```java
public void inheritFormData(FormInstance target, FormInstance source, NodeFormConfig config) {
    switch (config.getDataInheritStrategy()) {
        case INHERIT_ALL:
            // 复制 source 的所有字段值到 target
            copyAllFields(source, target);
            break;
        case INHERIT_SELECTED:
            // 只复制 config.getInheritFields() 指定的字段
            copySelectedFields(source, target, config.getInheritFields());
            break;
        case FRESH_START:
            // 不复制，使用表单模板的 defaultValue
            applyDefaultValues(target);
            break;
    }
}
```

### 6.3 变量同步时机

| 时机 | 说明 | 适用场景 |
|---|---|---|
| `START` | 流程启动时同步 | 启动表单的关键字段 |
| `SUBMIT` | 节点提交时同步 | 大部分场景 |
| `ALWAYS` | 字段值变化时实时同步 | 需要即时影响其他节点的字段 |

同步实现：
```java
@Component
public class FormSyncService {
    
    @Transactional(rollbackFor = Exception.class)
    public void syncToVariables(String formInstanceId, String processInstanceId) {
        // 1. 查表单实例的所有字段值
        List<FormFieldValue> values = formFieldValueMapper
            .selectByFormInstanceId(formInstanceId);
        
        // 2. 查映射配置
        List<FormFieldMapping> mappings = mappingMapper
            .selectByProcessDefinitionId(processDefinitionId);
        
        // 3. 同步到 Flowable 变量
        RuntimeService runtimeService = ...;
        for (FormFieldValue value : values) {
            FormFieldMapping mapping = findMapping(mappings, value.getFieldId());
            if (mapping != null) {
                Object typedValue = convertType(value.getVal(), mapping.getVariableType());
                runtimeService.setVariable(processInstanceId, mapping.getVariableName(), typedValue);
            }
        }
    }
}
```

---

## 七、节点表单配置

### 7.1 字段权限控制

节点配置决定用户在该节点看到的表单形态：

```json
{
  "visible_fields": ["amount", "reason", "attachment"],
  "editable_fields": ["reason"],
  "required_fields": ["reason"]
}
```

渲染逻辑：
- `visible_fields` 为 NULL → 全部字段可见
- `visible_fields` 为数组 → 仅数组中的字段可见
- 字段在 `editable_fields` 中 → 可编辑，否则只读
- 字段在 `required_fields` 中 → 强制必填（覆盖表单模板的 `required` 配置）

### 7.2 前端数据结构

节点激活时，后端返回：
```json
{
  "formId": "form_123",
  "formVersion": "3",
  "formInstanceId": "inst_456",
  "readMode": "PARTIAL",
  "fields": [
    {
      "fieldId": "amount",
      "title": "金额",
      "type": "NUMBER",
      "value": 15000,
      "visible": true,
      "editable": false,
      "required": false
    },
    {
      "fieldId": "reason",
      "title": "审批意见",
      "type": "TEXTAREA",
      "value": "",
      "visible": true,
      "editable": true,
      "required": true
    }
  ],
  "linkageRules": [...]
}
```

---

## 八、列表检索设计

### 8.1 检索字段配置

流程定义中标记哪些表单字段可作为列表检索条件：

```sql
-- 扩展 cat_process_form_field_mapping，增加检索配置
ALTER TABLE cat_process_form_field_mapping ADD COLUMN (
    searchable      VARCHAR(1) DEFAULT '0' COMMENT '是否可检索',
    search_type     VARCHAR(32) COMMENT '检索类型：EXACT/MATCH/RANGE/DATE_RANGE'
);
```

### 8.2 检索实现

列表查询走表单实例表联查：

```sql
SELECT 
    pi.*,
    fd.amount,
    fd.level
FROM cat_process_instance pi
LEFT JOIN cat_dynamic_form_instance fi 
    ON pi.id = fi.process_instance_id AND fi.node_key = '_start'
LEFT JOIN (
    SELECT 
        form_instance_id,
        MAX(CASE WHEN form_field_id = 'amount' THEN val END) as amount,
        MAX(CASE WHEN form_field_id = 'level' THEN val END) as level
    FROM cat_dynamic_form_field_value
    WHERE form_field_id IN ('amount', 'level')
    GROUP BY form_instance_id
) fd ON fi.id = fd.form_instance_id
WHERE pi.deleted = '0'
  AND fd.amount > 10000
ORDER BY pi.create_time DESC
```

### 8.3 性能优化（后续迭代）

高频检索字段建立冗余索引表：

```sql
CREATE TABLE cat_process_instance_index (
    id                  BIGINT PRIMARY KEY,
    process_instance_id INT NOT NULL,
    field_id            VARCHAR(64) NOT NULL,
    field_value         VARCHAR(512) NOT NULL,
    INDEX idx_query (field_id, field_value, process_instance_id)
);
```

---

## 九、回退与并行处理策略

### 9.1 回退时的表单数据

回退触发时：

```java
public void onBack(ProcessHandleParam param) {
    String processInstanceId = ...;
    String targetNodeKey = param.getTargetNodeId();
    
    // 1. 查询目标节点的最新快照
    FormSnapshot snapshot = snapshotMapper
        .selectLatest(processInstanceId, targetNodeKey);
    
    // 2. 创建新的表单实例，恢复快照数据
    FormInstance newInstance = formInstanceService
        .createFromSnapshot(snapshot);
    
    // 3. 删除回退目标节点之后的所有表单实例（或标记废弃）
    formInstanceService.deleteAfterNode(processInstanceId, targetNodeKey);
}
```

### 9.2 并行节点数据合并

并行汇聚时，根据 `parallel_merge_strategy` 合并各分支的表单数据：

```java
public Object mergeFieldValues(List<FormInstance> parallelInstances, 
                                String fieldId, 
                                String mergeStrategy) {
    List<Object> values = parallelInstances.stream()
        .map(inst -> inst.getFieldValue(fieldId))
        .filter(Objects::nonNull)
        .toList();
    
    return switch (mergeStrategy) {
        case "LAST_SUBMIT" -> values.get(values.size() - 1);
        case "MAX" -> values.stream().mapToDouble(this::toDouble).max().orElse(0);
        case "MIN" -> values.stream().mapToDouble(this::toDouble).min().orElse(0);
        case "SUM" -> values.stream().mapToDouble(this::toDouble).sum();
        case "MANUAL" -> throw new IllegalStateException("需人工合并");
        default -> values.get(values.size() - 1);
    };
}
```

---

## 十、API 接口设计

### 10.1 流程定义-表单绑定管理

```java
// 绑定表单到流程（全局或节点）
POST /processDefinition/bindForm
{
  "processDefinitionId": 1,
  "nodeKey": "Activity_1",  // null 表示全局默认
  "formId": "form_123",
  "formVersion": "3"        // null 表示使用当前发布版本
}

// 配置节点表单权限
POST /processDefinition/nodeFormConfig
{
  "processDefinitionId": 1,
  "nodeKey": "Activity_1",
  "readMode": "PARTIAL",
  "visibleFields": ["amount", "reason"],
  "editableFields": ["reason"],
  "requiredFields": ["reason"],
  "dataInheritFrom": "Activity_0",
  "parallelMergeStrategy": "LAST_SUBMIT"
}

// 配置字段→变量映射
POST /processDefinition/fieldMapping
{
  "processDefinitionId": 1,
  "formId": "form_123",
  "mappings": [
    {
      "fieldId": "amount",
      "variableName": "amount",
      "variableType": "DOUBLE",
      "syncTiming": "SUBMIT",
      "searchable": "1",
      "searchType": "RANGE"
    }
  ]
}
```

### 10.2 流程实例-表单交互

```java
// 获取当前节点的表单（含权限控制）
POST /processInstance/nodeForm
{
  "processInstanceId": 100,
  "taskId": "task_456"
}
// 返回：带 visible/editable/required 标记的表单结构

// 提交节点表单
POST /processInstance/submitNodeForm
{
  "processInstanceId": 100,
  "taskId": "task_456",
  "formData": {
    "reason": "同意",
    "amount": 15000
  }
}
// 后端：保存表单 → 同步变量 → 生成快照 → 完成任务

// 获取表单历史快照
POST /processInstance/formSnapshots
{
  "processInstanceId": 100,
  "nodeKey": "Activity_1"
}
```

### 10.3 列表检索

```java
// 流程实例列表（支持表单字段检索）
POST /processInstance/queryPage
{
  "page": 1,
  "size": 20,
  "formFilters": [
    {
      "fieldId": "amount",
      "operator": "GT",
      "value": 10000
    },
    {
      "fieldId": "level",
      "operator": "EQ",
      "value": "high"
    }
  ],
  "sortField": "createTime",
  "sortOrder": "DESC"
}
```

---

## 十一、关键业务流程时序

### 11.1 流程启动 + 填写启动表单

```
用户                  前端                  后端                  Flowable                 数据库
 │                     │                     │                        │                        │
 │── 选择流程模板 ────▶│                     │                        │                        │
 │                     │── GET /deployList ─▶│                        │                        │
 │                     │◀───────────────────│                        │                        │
 │                     │                     │                        │                        │
 │── 点击启动 ────────▶│                     │                        │                        │
 │                     │── POST /start ─────▶│                        │                        │
 │                     │                     │── 1.创建流程实例 ──────▶│                        │
 │                     │                     │◀── 返回 instanceId ────│                        │
 │                     │                     │                        │                        │
 │                     │                     │── 2.创建全局表单实例 ──────────────────────────────▶│
 │                     │                     │   (node_key = '_start')                         │
 │                     │                     │                        │                        │
 │                     │◀───────────────────│ 返回 processInstance   │                        │
 │                     │                     │                        │                        │
 │◀────────────────────│ 跳转表单填写页      │                        │                        │
 │                     │                     │                        │                        │
 │── 填写表单 ────────▶│                     │                        │                        │
 │                     │── GET /nodeForm ───▶│                        │                        │
 │                     │                     │── 3.查表单模板+配置 ─────────────────────────────▶│
 │                     │                     │                        │                        │
 │                     │◀───────────────────│ 返回表单结构（含权限）  │                        │
 │                     │                     │                        │                        │
 │◀────────────────────│ 渲染表单            │                        │                        │
 │                     │                     │                        │                        │
 │── 提交表单 ────────▶│                     │                        │                        │
 │                     │── POST /submit ────▶│                        │                        │
 │                     │                     │── 4.保存表单数据 ─────────────────────────────────▶│
 │                     │                     │── 5.同步关键字段到变量 ─▶│                        │
 │                     │                     │   runtimeService.setVariable()                  │
 │                     │                     │── 6.生成快照 ─────────────────────────────────────▶│
 │                     │                     │── 7.完成任务 ──────────▶│                        │
 │                     │                     │   taskService.complete()                        │
 │                     │                     │                        │                        │
 │                     │◀───────────────────│ 成功                   │                        │
 │                     │                     │                        │                        │
 │◀────────────────────│ 跳转待办列表        │                        │                        │
```

### 11.2 审批节点处理

```
用户                  前端                  后端                  Flowable                 数据库
 │                     │                     │                        │                        │
 │── 打开待办 ────────▶│                     │                        │                        │
 │                     │── GET /info ───────▶│                        │                        │
 │                     │                     │── 1.查流程实例信息      │                        │
 │                     │                     │── 2.查当前任务          │──▶                     │
 │                     │                     │                        │                        │
 │                     │                     │── 3.查节点表单配置 ───────────────────────────────▶│
 │                     │                     │── 4.创建/查节点表单实例 ──────────────────────────▶│
 │                     │                     │── 5.继承上游数据        │                        │
 │                     │                     │── 6.应用字段权限        │                        │
 │                     │                     │                        │                        │
 │                     │◀───────────────────│ 返回完整信息            │                        │
 │                     │                     │                        │                        │
 │◀────────────────────│ 渲染表单+审批按钮   │                        │                        │
 │                     │                     │                        │                        │
 │── 填写意见+通过 ───▶│                     │                        │                        │
 │                     │── POST /pass ──────▶│                        │                        │
 │                     │                     │── 7.保存表单（如有修改）───────────────────────────▶│
 │                     │                     │── 8.同步变量（如有修改）──▶                        │
 │                     │                     │── 9.生成快照 ─────────────────────────────────────▶│
 │                     │                     │── 10.记录审批意见 ─────────────────────────────────▶│
 │                     │                     │── 11.完成任务 ─────────▶│                        │
 │                     │                     │   taskService.complete()                        │
 │                     │                     │                        │                        │
 │                     │◀───────────────────│ 成功                   │                        │
 │                     │                     │                        │                        │
 │◀────────────────────│ 跳转下一页          │                        │                        │
```

---

## 十二、后续迭代规划

| 优先级 | 功能 | 说明 |
|---|---|---|
| P1 | 子表单/明细表 | 动态行表格（报销明细等），支持聚合函数 |
| P1 | 附件权限管理 | 不同节点对附件的可见/下载/删除权限 |
| P1 | 数据权限 | 列表页按角色过滤（全部/本部门/仅自己） |
| P2 | 抄送/知会 | 纯知会节点，不参与流转 |
| P2 | 批量审批 | 列表页一键通过/拒绝（仅只读表单） |
| P2 | 审批意见关联字段 | 对具体字段的批注 |
| P2 | 定时任务/超时自动流转 | 超时自动通过/拒绝 |
| P3 | 表单打印/导出 | PDF 生成，支持水印、电子签章 |
| P3 | 流程嵌套/子流程 | 主流程调用子流程，数据互通 |
| P3 | 历史数据归档 | 按年度分表或迁移到 ES |

---

## 十三、风险与应对

| 风险 | 影响 | 应对 |
|---|---|---|
| 变量同步失败导致网关条件无法判断 | 流程卡死 | 同步与表单提交在同事务内；失败回滚 |
| 并行节点改同一字段导致数据覆盖 | 条件判断错误 | 并行合并策略可配置；冲突时标记人工处理 |
| 表单版本升级导致运行中实例异常 | 数据不一致 | 实例锁定绑定时的表单版本 |
| 大量表单字段导致列表查询慢 | 用户体验差 | 高频检索字段建冗余索引表（后续迭代） |
| God Class 风险（DefaultRouteService） | 维护困难 | 分层架构：简单条件走原生，复杂条件分散到各 Bean |

---

*设计日期：2026-05-20*
*状态：待实现*
