# 流程实例绑定表单 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将流程定义阶段的表单绑定配置落地到流程实例运行时，实现发起/草稿填写表单、任务流转时校验并写入表单数据、查看详情时返回带分组结构和权限的完整表单渲染配置。

**Architecture:** 新建关联表 `cat_process_instance_form` 桥接流程实例与表单实例。提取统一的 `ProcessFormService` 封装表单配置解析（resolveFormConfig）、表单实例创建/写入、权限校验、渲染数据组装等核心逻辑，供 StartProcessCommand、PassTaskCommand、saveDraft、info、ApprovalTaskCreateListener 调用。

**Tech Stack:** Spring Boot 3, Java 21, MyBatis-Plus, Flowable 8, MySQL 8

---

## File Structure

### New files

| File | Responsibility |
|---|---|
| `common/src/main/java/com/cat/common/entity/process/ProcessInstanceForm.java` | 关联表实体类 |
| `simple/src/main/java/com/cat/simple/process/mapper/ProcessInstanceFormMapper.java` | 关联表 Mapper |
| `common/src/main/java/com/cat/common/entity/process/TaskFormVO.java` | `/info` 返回的表单渲染数据 VO |
| `common/src/main/java/com/cat/common/entity/process/TaskFormFieldVO.java` | 字段渲染 VO（含 permission + value） |
| `common/src/main/java/com/cat/common/entity/process/TaskFormGroupVO.java` | 分组渲染 VO |
| `common/src/main/java/com/cat/common/entity/process/TaskFormInheritedVO.java` | 继承表单渲染 VO |
| `common/src/main/java/com/cat/common/entity/process/StartProcessParam.java` | 发起流程请求体 DTO |
| `common/src/main/java/com/cat/common/entity/process/SaveDraftParam.java` | 保存草稿请求体 DTO |
| `simple/src/main/java/com/cat/simple/process/service/ProcessFormService.java` | 流程表单服务接口 |
| `simple/src/main/java/com/cat/simple/process/service/impl/ProcessFormServiceImpl.java` | 流程表单服务实现（核心） |
| `sql/process_instance_form.sql` | 建表 DDL |

### Modified files

| File | Change |
|---|---|
| `common/src/main/java/com/cat/common/entity/process/ProcessHandleParam.java` | 增加 `formData` 字段 |
| `common/src/main/java/com/cat/common/entity/process/ProcessInstance.java` | 增加 `taskForm` transient 字段 |
| `simple/src/main/java/com/cat/simple/process/controller/ProcessInstanceController.java` | start/saveDraft 改用 @RequestBody DTO |
| `simple/src/main/java/com/cat/simple/process/service/ProcessInstanceService.java` | start/saveDraft 签名增加 formData |
| `simple/src/main/java/com/cat/simple/process/service/impl/ProcessInstanceServiceImpl.java` | 调用 ProcessFormService |
| `simple/src/main/java/com/cat/simple/config/flowable/command/StartProcessCommand.java` | 构造函数增加 formData，beforeHook 写入表单 |
| `simple/src/main/java/com/cat/simple/config/flowable/hook/StartContext.java` | 增加 formData 字段 |
| `simple/src/main/java/com/cat/simple/config/flowable/command/PassTaskCommand.java` | beforeHook 中校验+写入表单 |
| `simple/src/main/java/com/cat/simple/config/flowable/hook/PassContext.java` | 已有 formData，无需改动 |
| `simple/src/main/java/com/cat/simple/config/flowable/listener/ApprovalTaskCreateListener.java` | 任务到达时创建节点表单实例 |
| `simple/src/test/java/com/cat/simple/service/ProcessInstanceServiceTest.java` | 补充表单相关测试 |

---

## Task 1: Create ProcessInstanceForm entity and mapper

**Files:**
- Create: `sql/process_instance_form.sql`
- Create: `common/src/main/java/com/cat/common/entity/process/ProcessInstanceForm.java`
- Create: `simple/src/main/java/com/cat/simple/process/mapper/ProcessInstanceFormMapper.java`

- [ ] **Step 1: Write the SQL DDL**

Create `sql/process_instance_form.sql`:

```sql
CREATE TABLE IF NOT EXISTS `cat_process_instance_form` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `process_instance_id` INT           NOT NULL COMMENT '流程实例ID',
    `node_id`            VARCHAR(64)    NULL     COMMENT '节点ID，null=主表单',
    `form_id`            VARCHAR(64)    NOT NULL COMMENT '表单ID',
    `form_version`       VARCHAR(16)    NOT NULL COMMENT '表单版本（启动时快照）',
    `form_instance_id`   VARCHAR(64)    NOT NULL COMMENT '表单实例ID',
    `create_by`          VARCHAR(64)    NULL     COMMENT '创建人',
    `create_time`        DATETIME       NULL     COMMENT '创建时间',
    UNIQUE KEY `uk_instance_node` (`process_instance_id`, `node_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流程实例表单关联' ROW_FORMAT = Dynamic;
```

- [ ] **Step 2: Create the entity class**

Create `common/src/main/java/com/cat/common/entity/process/ProcessInstanceForm.java`:

```java
package com.cat.common.entity.process;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_process_instance_form")
@Schema(name = "ProcessInstanceForm", description = "流程实例表单关联")
public class ProcessInstanceForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "流程实例ID")
    private Integer processInstanceId;

    @Schema(description = "节点ID，null=主表单")
    private String nodeId;

    @Schema(description = "表单ID")
    private String formId;

    @Schema(description = "表单版本（启动时快照）")
    private String formVersion;

    @Schema(description = "表单实例ID")
    private String formInstanceId;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
```

- [ ] **Step 3: Create the mapper**

Create `simple/src/main/java/com/cat/simple/process/mapper/ProcessInstanceFormMapper.java`:

```java
package com.cat.simple.process.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.process.ProcessInstanceForm;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProcessInstanceFormMapper extends BaseMapper<ProcessInstanceForm> {
}
```

- [ ] **Step 4: Commit**

```bash
git add sql/process_instance_form.sql common/src/main/java/com/cat/common/entity/process/ProcessInstanceForm.java simple/src/main/java/com/cat/simple/process/mapper/ProcessInstanceFormMapper.java
git commit -m "feat: add ProcessInstanceForm entity and mapper for instance-form binding"
```

---

## Task 2: Create TaskFormVO and related VO classes

**Files:**
- Create: `common/src/main/java/com/cat/common/entity/process/TaskFormFieldVO.java`
- Create: `common/src/main/java/com/cat/common/entity/process/TaskFormGroupVO.java`
- Create: `common/src/main/java/com/cat/common/entity/process/TaskFormInheritedVO.java`
- Create: `common/src/main/java/com/cat/common/entity/process/TaskFormVO.java`

- [ ] **Step 1: Create TaskFormFieldVO**

Create `common/src/main/java/com/cat/common/entity/process/TaskFormFieldVO.java`:

```java
package com.cat.common.entity.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "任务表单字段渲染数据")
public class TaskFormFieldVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "字段标识")
    private String fieldKey;

    @Schema(description = "字段标题")
    private String label;

    @Schema(description = "字段类型")
    private String type;

    @Schema(description = "字段权限：VISIBLE/READONLY/HIDDEN/EDITABLE/REQUIRED")
    private String permission;

    @Schema(description = "当前值")
    private Object value;

    @Schema(description = "是否必填")
    private boolean required;

    @Schema(description = "选项列表")
    private List<?> options;

    @Schema(description = "来源表单ID（仅继承字段有值）")
    private String sourceFormId;
}
```

- [ ] **Step 2: Create TaskFormGroupVO**

Create `common/src/main/java/com/cat/common/entity/process/TaskFormGroupVO.java`:

```java
package com.cat.common.entity.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "任务表单分组渲染数据")
public class TaskFormGroupVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "分组ID")
    private String groupId;

    @Schema(description = "分组名称")
    private String name;

    @Schema(description = "分组描述")
    private String description;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "默认折叠 0展开 1折叠")
    private String collapsed;

    @Schema(description = "分组下的字段列表")
    private List<TaskFormFieldVO> fields;
}
```

- [ ] **Step 3: Create TaskFormInheritedVO**

Create `common/src/main/java/com/cat/common/entity/process/TaskFormInheritedVO.java`:

```java
package com.cat.common.entity.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "继承表单渲染数据")
public class TaskFormInheritedVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主表单ID")
    private String formId;

    @Schema(description = "主表单版本")
    private String formVersion;

    @Schema(description = "主表单实例ID")
    private String formInstanceId;

    @Schema(description = "未分组字段列表")
    private List<TaskFormFieldVO> formFields;

    @Schema(description = "分组字段列表")
    private List<TaskFormGroupVO> groups;
}
```

- [ ] **Step 4: Create TaskFormVO**

Create `common/src/main/java/com/cat/common/entity/process/TaskFormVO.java`:

```java
package com.cat.common.entity.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "任务表单渲染数据")
public class TaskFormVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "当前用户是否可编辑（是否为任务处理人）")
    private boolean editable;

    @Schema(description = "当前节点绑定的表单ID")
    private String formId;

    @Schema(description = "表单版本")
    private String formVersion;

    @Schema(description = "表单实例ID")
    private String formInstanceId;

    @Schema(description = "未分组字段列表")
    private List<TaskFormFieldVO> formFields;

    @Schema(description = "分组字段列表")
    private List<TaskFormGroupVO> groups;

    @Schema(description = "继承的主表单数据（仅 inheritMainForm=1 时有值）")
    private TaskFormInheritedVO inherited;
}
```

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/cat/common/entity/process/TaskFormFieldVO.java common/src/main/java/com/cat/common/entity/process/TaskFormGroupVO.java common/src/main/java/com/cat/common/entity/process/TaskFormInheritedVO.java common/src/main/java/com/cat/common/entity/process/TaskFormVO.java
git commit -m "feat: add TaskFormVO and related VOs for task form rendering"
```

---

## Task 3: Add formData to ProcessHandleParam and ProcessInstance

**Files:**
- Modify: `common/src/main/java/com/cat/common/entity/process/ProcessHandleParam.java`
- Modify: `common/src/main/java/com/cat/common/entity/process/ProcessInstance.java`

- [ ] **Step 1: Add formData to ProcessHandleParam**

Add field to `ProcessHandleParam`:

```java
@Schema(description = "表单数据")
private Map<String, Object> formData;
```

Add import: `import java.util.Map;`

- [ ] **Step 2: Add taskForm to ProcessInstance**

Add transient field to `ProcessInstance`:

```java
@Schema(description = "任务表单渲染数据")
@TableField(exist = false)
private TaskFormVO taskForm;
```

Add import: `import com.cat.common.entity.process.TaskFormVO;`

- [ ] **Step 3: Commit**

```bash
git add common/src/main/java/com/cat/common/entity/process/ProcessHandleParam.java common/src/main/java/com/cat/common/entity/process/ProcessInstance.java
git commit -m "feat: add formData to ProcessHandleParam, taskForm to ProcessInstance"
```

---

## Task 4: Create ProcessFormService — core logic

This is the central service that encapsulates all form-instance-process binding logic.

**Files:**
- Create: `simple/src/main/java/com/cat/simple/process/service/ProcessFormService.java`
- Create: `simple/src/main/java/com/cat/simple/process/service/impl/ProcessFormServiceImpl.java`

- [ ] **Step 1: Create the service interface**

Create `simple/src/main/java/com/cat/simple/process/service/ProcessFormService.java`:

```java
package com.cat.simple.process.service;

import com.cat.common.entity.process.TaskFormVO;

import java.util.Map;

public interface ProcessFormService {

    /**
     * 创建表单实例并初始化字段实例，写入关联表。
     * 如果关联表已存在该节点的记录，则复用已有的表单实例（驳回场景）。
     *
     * @param processInstanceId 流程实例ID
     * @param processDefinitionId 流程定义ID
     * @param nodeId 节点ID（startEvent 传其 nodeId，主表单传 null）
     * @return 关联记录，无绑定表单时返回 null
     */
    com.cat.common.entity.process.ProcessInstanceForm createFormInstanceIfNeeded(
            Integer processInstanceId, Integer processDefinitionId, String nodeId);

    /**
     * 写入表单数据，按字段权限过滤。
     * start/saveDraft 场景：skipRequired=true（不校验必填）。
     * pass 场景：skipRequired=false（校验必填）。
     *
     * @param processInstanceId 流程实例ID
     * @param processDefinitionId 流程定义ID
     * @param nodeId 节点ID
     * @param formData 前端提交的表单数据
     * @param skipRequired 是否跳过必填校验（草稿跳过）
     */
    void writeFormData(Integer processInstanceId, Integer processDefinitionId,
                       String nodeId, Map<String, Object> formData, boolean skipRequired);

    /**
     * 组装任务表单渲染数据（含字段定义、权限、已填值、分组、继承）。
     *
     * @param processInstanceId 流程实例ID
     * @param processDefinitionId 流程定义ID
     * @param nodeId 节点ID
     * @param editable 当前用户是否可编辑
     * @return 表单渲染数据，无绑定表单时返回 null
     */
    TaskFormVO buildTaskForm(Integer processInstanceId, Integer processDefinitionId,
                             String nodeId, boolean editable);
}
```

- [ ] **Step 2: Create ProcessFormServiceImpl — resolveFormConfig and createFormInstanceIfNeeded**

Create `simple/src/main/java/com/cat/simple/process/service/impl/ProcessFormServiceImpl.java`.

Internal data class `FormConfig` to hold resolved configuration:

```java
@Data
@AllArgsConstructor
private static class FormConfig {
    private ProcessDefinitionForm nodeBinding;   // 节点绑定，可能为 null
    private ProcessDefinitionForm globalBinding; // 全局绑定，可能为 null
    private boolean inheritMainForm;
    private List<ProcessNodeFieldPermission> fieldPermissions;
}
```

`resolveFormConfig` method:

```java
private FormConfig resolveFormConfig(Integer processDefinitionId, String nodeId) {
    ProcessDefinition definition = processDefinitionMapper.selectById(processDefinitionId);
    if (definition == null) {
        return null;
    }
    String version = definition.getCurrentVersion();

    // 1. 查全局绑定
    ProcessDefinitionForm globalBinding = processDefinitionFormMapper.selectOne(
            new LambdaQueryWrapper<ProcessDefinitionForm>()
                    .eq(ProcessDefinitionForm::getProcessDefinitionId, processDefinitionId)
                    .eq(ProcessDefinitionForm::getVersion, version)
                    .eq(ProcessDefinitionForm::getBindType, "GLOBAL"));

    // 2. 查节点绑定
    ProcessDefinitionForm nodeBinding = null;
    if (nodeId != null) {
        nodeBinding = processDefinitionFormMapper.selectOne(
                new LambdaQueryWrapper<ProcessDefinitionForm>()
                        .eq(ProcessDefinitionForm::getProcessDefinitionId, processDefinitionId)
                        .eq(ProcessDefinitionForm::getVersion, version)
                        .eq(ProcessDefinitionForm::getBindType, "NODE")
                        .eq(ProcessDefinitionForm::getNodeId, nodeId));
    }

    // 3. 确定最终表单来源和 inheritMainForm
    boolean inheritMainForm = false;
    if (nodeBinding != null) {
        inheritMainForm = "1".equals(nodeBinding.getInheritMainForm());
    }

    // 4. 查字段权限
    List<ProcessNodeFieldPermission> permissions = Collections.emptyList();
    String effectiveNodeId = nodeBinding != null ? nodeId : null;
    if (effectiveNodeId != null) {
        permissions = processNodeFieldPermissionMapper.selectList(
                new LambdaQueryWrapper<ProcessNodeFieldPermission>()
                        .eq(ProcessNodeFieldPermission::getProcessDefinitionId, processDefinitionId)
                        .eq(ProcessNodeFieldPermission::getVersion, version)
                        .eq(ProcessNodeFieldPermission::getNodeId, effectiveNodeId));
    }

    // 无任何绑定
    if (nodeBinding == null && globalBinding == null) {
        return null;
    }

    return new FormConfig(nodeBinding, globalBinding, inheritMainForm, permissions);
}
```

`createFormInstanceIfNeeded` method:

```java
@Override
@Transactional(rollbackFor = Exception.class)
public ProcessInstanceForm createFormInstanceIfNeeded(Integer processInstanceId,
        Integer processDefinitionId, String nodeId) {
    FormConfig config = resolveFormConfig(processDefinitionId, nodeId);
    if (config == null) {
        return null;
    }

    // 确定表单来源
    ProcessDefinitionForm binding = config.getNodeBinding() != null
            ? config.getNodeBinding() : config.getGlobalBinding();

    // 查关联表是否已存在
    ProcessInstanceForm existing = processInstanceFormMapper.selectOne(
            new LambdaQueryWrapper<ProcessInstanceForm>()
                    .eq(ProcessInstanceForm::getProcessInstanceId, processInstanceId)
                    .eq(nodeId != null,
                            ProcessInstanceForm::getNodeId, nodeId)
                    .isNull(nodeId == null,
                            ProcessInstanceForm::getNodeId));
    if (existing != null) {
        return existing; // 驳回场景，复用
    }

    // 创建 DynamicFormInstance
    String userId = guard.getCurrentUserId();
    LocalDateTime now = LocalDateTime.now();
    DynamicFormInstance formInstance = new DynamicFormInstance()
            .setFormId(binding.getFormId())
            .setVersion(binding.getFormVersion())
            .setCreateBy(userId)
            .setDeleted("0")
            .setCreateTime(now)
            .setUpdateTime(now);
    dynamicFormInstanceMapper.insert(formInstance);

    // 初始化所有字段实例（值为 null）
    List<DynamicFormField> fields = dynamicFormFieldMapper.selectList(
            new LambdaQueryWrapper<DynamicFormField>()
                    .eq(DynamicFormField::getFormId, binding.getFormId())
                    .eq(DynamicFormField::getVersion, binding.getFormVersion())
                    .orderByAsc(DynamicFormField::getSort));
    List<DynamicFormFieldInstance> fieldInstances = new ArrayList<>();
    for (DynamicFormField field : fields) {
        DynamicFormFieldInstance fi = new DynamicFormFieldInstance()
                .setFormFieldId(field.getId())
                .setFormInstanceId(formInstance.getId())
                .setVersion(binding.getFormVersion())
                .setVal(null)
                .setCreateBy(userId)
                .setDeleted("0")
                .setCreateTime(now)
                .setUpdateTime(now);
        fieldInstances.add(fi);
    }
    if (!fieldInstances.isEmpty()) {
        dynamicFormFieldInstanceMapper.insertOrUpdate(fieldInstances);
    }

    // 如果 inheritMainForm=1，也要创建主表单实例
    if (config.isInheritMainForm() && config.getGlobalBinding() != null) {
        ProcessInstanceForm existingGlobal = processInstanceFormMapper.selectOne(
                new LambdaQueryWrapper<ProcessInstanceForm>()
                        .eq(ProcessInstanceForm::getProcessInstanceId, processInstanceId)
                        .isNull(ProcessInstanceForm::getNodeId));
        if (existingGlobal == null) {
            ProcessDefinitionForm globalBinding = config.getGlobalBinding();
            DynamicFormInstance globalFormInstance = new DynamicFormInstance()
                    .setFormId(globalBinding.getFormId())
                    .setVersion(globalBinding.getFormVersion())
                    .setCreateBy(userId)
                    .setDeleted("0")
                    .setCreateTime(now)
                    .setUpdateTime(now);
            dynamicFormInstanceMapper.insert(globalFormInstance);

            List<DynamicFormField> globalFields = dynamicFormFieldMapper.selectList(
                    new LambdaQueryWrapper<DynamicFormField>()
                            .eq(DynamicFormField::getFormId, globalBinding.getFormId())
                            .eq(DynamicFormField::getVersion, globalBinding.getFormVersion())
                            .orderByAsc(DynamicFormField::getSort));
            List<DynamicFormFieldInstance> globalFieldInstances = new ArrayList<>();
            for (DynamicFormField field : globalFields) {
                DynamicFormFieldInstance fi = new DynamicFormFieldInstance()
                        .setFormFieldId(field.getId())
                        .setFormInstanceId(globalFormInstance.getId())
                        .setVersion(globalBinding.getFormVersion())
                        .setVal(null)
                        .setCreateBy(userId)
                        .setDeleted("0")
                        .setCreateTime(now)
                        .setUpdateTime(now);
                globalFieldInstances.add(fi);
            }
            if (!globalFieldInstances.isEmpty()) {
                dynamicFormFieldInstanceMapper.insertOrUpdate(globalFieldInstances);
            }

            // 写入主表单关联
            ProcessInstanceForm globalRel = new ProcessInstanceForm()
                    .setProcessInstanceId(processInstanceId)
                    .setNodeId(null)
                    .setFormId(globalBinding.getFormId())
                    .setFormVersion(globalBinding.getFormVersion())
                    .setFormInstanceId(globalFormInstance.getId())
                    .setCreateBy(userId)
                    .setCreateTime(now);
            processInstanceFormMapper.insert(globalRel);
        }
    }

    // 写入节点/当前表单关联
    ProcessInstanceForm rel = new ProcessInstanceForm()
            .setProcessInstanceId(processInstanceId)
            .setNodeId(nodeId)
            .setFormId(binding.getFormId())
            .setFormVersion(binding.getFormVersion())
            .setFormInstanceId(formInstance.getId())
            .setCreateBy(userId)
            .setCreateTime(now);
    processInstanceFormMapper.insert(rel);
    return rel;
}
```

Required injections:

```java
@Resource private ProcessInstanceFormMapper processInstanceFormMapper;
@Resource private ProcessDefinitionFormMapper processDefinitionFormMapper;
@Resource private ProcessNodeFieldPermissionMapper processNodeFieldPermissionMapper;
@Resource private DynamicFormInstanceMapper dynamicFormInstanceMapper;
@Resource private DynamicFormFieldInstanceMapper dynamicFormFieldInstanceMapper;
@Resource private DynamicFormFieldMapper dynamicFormFieldMapper;
@Resource private DynamicFormFieldGroupMapper dynamicFormFieldGroupMapper;
@Resource private ProcessDefinitionMapper processDefinitionMapper;
@Resource private ProcessGuard guard;
```

- [ ] **Step 3: Implement writeFormData**

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void writeFormData(Integer processInstanceId, Integer processDefinitionId,
                          String nodeId, Map<String, Object> formData, boolean skipRequired) {
    if (formData == null || formData.isEmpty()) {
        return;
    }
    FormConfig config = resolveFormConfig(processDefinitionId, nodeId);
    if (config == null) {
        return;
    }

    ProcessDefinitionForm binding = config.getNodeBinding() != null
            ? config.getNodeBinding() : config.getGlobalBinding();

    // 查关联表获取表单实例ID
    ProcessInstanceForm rel = processInstanceFormMapper.selectOne(
            new LambdaQueryWrapper<ProcessInstanceForm>()
                    .eq(ProcessInstanceForm::getProcessInstanceId, processInstanceId)
                    .eq(nodeId != null, ProcessInstanceForm::getNodeId, nodeId)
                    .isNull(nodeId == null, ProcessInstanceForm::getNodeId));
    if (rel == null) {
        return;
    }

    // 构建字段权限 map：fieldKey → permission
    Map<String, String> permissionMap = config.getFieldPermissions().stream()
            .collect(Collectors.toMap(
                    ProcessNodeFieldPermission::getFieldKey,
                    ProcessNodeFieldPermission::getPermission,
                    (a, b) -> a));

    // 加载字段定义
    List<DynamicFormField> fields = dynamicFormFieldMapper.selectList(
            new LambdaQueryWrapper<DynamicFormField>()
                    .eq(DynamicFormField::getFormId, binding.getFormId())
                    .eq(DynamicFormField::getVersion, binding.getFormVersion())
                    .orderByAsc(DynamicFormField::getSort));

    // 加载已有字段实例
    List<DynamicFormFieldInstance> existInstances = dynamicFormFieldInstanceMapper.selectList(
            new LambdaQueryWrapper<DynamicFormFieldInstance>()
                    .eq(DynamicFormFieldInstance::getFormInstanceId, rel.getFormInstanceId()));
    Map<String, DynamicFormFieldInstance> existMap = existInstances.stream()
            .collect(Collectors.toMap(
                    DynamicFormFieldInstance::getFormFieldId,
                    Function.identity(),
                    (a, b) -> a));

    String userId = guard.getCurrentUserId();
    LocalDateTime now = LocalDateTime.now();
    List<DynamicFormFieldInstance> toSave = new ArrayList<>();

    for (DynamicFormField field : fields) {
        String fieldKey = field.getFieldId();
        String permission = permissionMap.getOrDefault(fieldKey, null);

        // READONLY / HIDDEN 字段忽略前端传值
        if ("READONLY".equals(permission) || "HIDDEN".equals(permission)) {
            continue;
        }

        // 未配置权限的字段，使用表单模板默认配置（按 required 判断）
        if (permission == null) {
            // 无权限配置时，字段按模板自身配置决定是否可编辑
            // 仍然接受前端传值
        }

        // REQUIRED 校验
        if ("REQUIRED".equals(permission) && !skipRequired) {
            Object val = formData.get(fieldKey);
            if (val == null || (val instanceof String s && s.isBlank())) {
                throw new IllegalStateException("必填字段未填写: " + field.getTitle());
            }
        }

        Object submittedValue = formData.get(fieldKey);
        if (submittedValue == null) {
            continue; // 前端未传该字段值
        }

        DynamicFormFieldInstance fi = existMap.get(field.getId());
        if (fi == null) {
            fi = new DynamicFormFieldInstance()
                    .setFormFieldId(field.getId())
                    .setFormInstanceId(rel.getFormInstanceId())
                    .setVersion(binding.getFormVersion())
                    .setCreateBy(userId)
                    .setDeleted("0")
                    .setCreateTime(now);
        }
        fi.setVal(submittedValue)
                .setUpdateTime(now);
        toSave.add(fi);
    }

    if (!toSave.isEmpty()) {
        dynamicFormFieldInstanceMapper.insertOrUpdate(toSave);
    }

    // 如果 inheritMainForm=1，也要写入主表单数据
    if (config.isInheritMainForm() && config.getGlobalBinding() != null) {
        ProcessInstanceForm globalRel = processInstanceFormMapper.selectOne(
                new LambdaQueryWrapper<ProcessInstanceForm>()
                        .eq(ProcessInstanceForm::getProcessInstanceId, processInstanceId)
                        .isNull(ProcessInstanceForm::getNodeId));
        if (globalRel != null) {
            writeFormDataForBinding(globalRel, config.getGlobalBinding(),
                    config.getFieldPermissions(), formData, skipRequired, userId, now);
        }
    }
}
```

Helper `writeFormDataForBinding` for the inherited global form write:

```java
private void writeFormDataForBinding(ProcessInstanceForm rel, ProcessDefinitionForm binding,
        List<ProcessNodeFieldPermission> allPermissions, Map<String, Object> formData,
        boolean skipRequired, String userId, LocalDateTime now) {
    Map<String, String> permissionMap = allPermissions.stream()
            .collect(Collectors.toMap(
                    ProcessNodeFieldPermission::getFieldKey,
                    ProcessNodeFieldPermission::getPermission,
                    (a, b) -> a));

    List<DynamicFormField> fields = dynamicFormFieldMapper.selectList(
            new LambdaQueryWrapper<DynamicFormField>()
                    .eq(DynamicFormField::getFormId, binding.getFormId())
                    .eq(DynamicFormField::getVersion, binding.getFormVersion()));

    List<DynamicFormFieldInstance> existInstances = dynamicFormFieldInstanceMapper.selectList(
            new LambdaQueryWrapper<DynamicFormFieldInstance>()
                    .eq(DynamicFormFieldInstance::getFormInstanceId, rel.getFormInstanceId()));
    Map<String, DynamicFormFieldInstance> existMap = existInstances.stream()
            .collect(Collectors.toMap(
                    DynamicFormFieldInstance::getFormFieldId,
                    Function.identity(),
                    (a, b) -> a));

    List<DynamicFormFieldInstance> toSave = new ArrayList<>();
    for (DynamicFormField field : fields) {
        String fieldKey = field.getFieldId();
        String permission = permissionMap.getOrDefault(fieldKey, null);
        if ("READONLY".equals(permission) || "HIDDEN".equals(permission)) {
            continue;
        }
        if ("REQUIRED".equals(permission) && !skipRequired) {
            Object val = formData.get(fieldKey);
            if (val == null || (val instanceof String s && s.isBlank())) {
                throw new IllegalStateException("必填字段未填写: " + field.getTitle());
            }
        }
        Object submittedValue = formData.get(fieldKey);
        if (submittedValue == null) continue;

        DynamicFormFieldInstance fi = existMap.get(field.getId());
        if (fi == null) {
            fi = new DynamicFormFieldInstance()
                    .setFormFieldId(field.getId())
                    .setFormInstanceId(rel.getFormInstanceId())
                    .setVersion(binding.getFormVersion())
                    .setCreateBy(userId)
                    .setDeleted("0")
                    .setCreateTime(now);
        }
        fi.setVal(submittedValue).setUpdateTime(now);
        toSave.add(fi);
    }
    if (!toSave.isEmpty()) {
        dynamicFormFieldInstanceMapper.insertOrUpdate(toSave);
    }
}
```

- [ ] **Step 4: Implement buildTaskForm**

```java
@Override
public TaskFormVO buildTaskForm(Integer processInstanceId, Integer processDefinitionId,
                                 String nodeId, boolean editable) {
    FormConfig config = resolveFormConfig(processDefinitionId, nodeId);
    if (config == null) {
        return null;
    }

    ProcessDefinitionForm binding = config.getNodeBinding() != null
            ? config.getNodeBinding() : config.getGlobalBinding();

    // 查关联表
    ProcessInstanceForm rel = processInstanceFormMapper.selectOne(
            new LambdaQueryWrapper<ProcessInstanceForm>()
                    .eq(ProcessInstanceForm::getProcessInstanceId, processInstanceId)
                    .eq(nodeId != null, ProcessInstanceForm::getNodeId, nodeId)
                    .isNull(nodeId == null, ProcessInstanceForm::getNodeId));
    if (rel == null) {
        return null;
    }

    // 构建字段权限 map
    Map<String, String> permissionMap = config.getFieldPermissions().stream()
            .collect(Collectors.toMap(
                    ProcessNodeFieldPermission::getFieldKey,
                    ProcessNodeFieldPermission::getPermission,
                    (a, b) -> a));

    // 构建当前表单的渲染数据
    TaskFormVO vo = new TaskFormVO();
    vo.setEditable(editable);
    vo.setFormId(binding.getFormId());
    vo.setFormVersion(binding.getFormVersion());
    vo.setFormInstanceId(rel.getFormInstanceId());

    // 加载表单字段和分组
    List<TaskFormFieldVO> formFields = new ArrayList<>();
    List<TaskFormGroupVO> groups = new ArrayList<>();
    buildFormFieldsAndGroups(binding.getFormId(), binding.getFormVersion(),
            rel.getFormInstanceId(), permissionMap, editable, null, formFields, groups);
    vo.setFormFields(formFields.isEmpty() ? null : formFields);
    vo.setGroups(groups.isEmpty() ? null : groups);

    // 构建继承表单
    if (config.isInheritMainForm() && config.getGlobalBinding() != null) {
        TaskFormInheritedVO inherited = buildInheritedForm(processInstanceId,
                config.getGlobalBinding(), permissionMap, editable);
        vo.setInherited(inherited);
    }

    return vo;
}
```

Helper `buildFormFieldsAndGroups`:

```java
private void buildFormFieldsAndGroups(String formId, String formVersion, String formInstanceId,
        Map<String, String> permissionMap, boolean editable, String sourceFormId,
        List<TaskFormFieldVO> formFields, List<TaskFormGroupVO> groups) {

    // 加载字段值
    Map<String, Object> valueMap = Collections.emptyMap();
    if (formInstanceId != null) {
        List<DynamicFormFieldInstance> instances = dynamicFormFieldInstanceMapper.selectList(
                new LambdaQueryWrapper<DynamicFormFieldInstance>()
                        .eq(DynamicFormFieldInstance::getFormInstanceId, formInstanceId));
        // fieldId -> val (需要通过 fieldId 映射)
        // 先加载字段定义做映射
    }

    // 加载字段定义
    List<DynamicFormField> fields = dynamicFormFieldMapper.selectList(
            new LambdaQueryWrapper<DynamicFormField>()
                    .eq(DynamicFormField::getFormId, formId)
                    .eq(DynamicFormField::getVersion, formVersion)
                    .orderByAsc(DynamicFormField::getSort));

    // 加载字段实例值：formFieldId -> val
    Map<String, Object> fieldInstanceValueMap = new HashMap<>();
    if (formInstanceId != null) {
        List<DynamicFormFieldInstance> instances = dynamicFormFieldInstanceMapper.selectList(
                new LambdaQueryWrapper<DynamicFormFieldInstance>()
                        .eq(DynamicFormFieldInstance::getFormInstanceId, formInstanceId));
        for (DynamicFormFieldInstance fi : instances) {
            fieldInstanceValueMap.put(fi.getFormFieldId(), fi.getVal());
        }
    }

    // 加载分组
    List<DynamicFormFieldGroup> groupList = dynamicFormFieldGroupMapper.selectList(
            new LambdaQueryWrapper<DynamicFormFieldGroup>()
                    .eq(DynamicFormFieldGroup::getFormId, formId)
                    .eq(DynamicFormFieldGroup::getVersion, formVersion)
                    .eq(DynamicFormFieldGroup::getDeleted, "0")
                    .orderByAsc(DynamicFormFieldGroup::getSort));

    // 分组 ID -> 字段列表
    Map<String, List<DynamicFormField>> groupFieldMap = fields.stream()
            .filter(f -> f.getGroupId() != null)
            .collect(Collectors.groupingBy(DynamicFormField::getGroupId, Collectors.toList()));

    // 未分组字段
    List<DynamicFormField> ungroupedFields = fields.stream()
            .filter(f -> f.getGroupId() == null)
            .collect(Collectors.toList());

    // 构建未分组字段 VO
    for (DynamicFormField field : ungroupedFields) {
        TaskFormFieldVO fieldVO = toFieldVO(field, permissionMap, editable,
                fieldInstanceValueMap.get(field.getId()), sourceFormId);
        formFields.add(fieldVO);
    }

    // 构建分组 VO
    for (DynamicFormFieldGroup group : groupList) {
        TaskFormGroupVO groupVO = new TaskFormGroupVO();
        groupVO.setGroupId(group.getId());
        groupVO.setName(group.getName());
        groupVO.setDescription(group.getDescription());
        groupVO.setSort(group.getSort());
        groupVO.setCollapsed(group.getCollapsed());

        List<DynamicFormField> groupFields = groupFieldMap.getOrDefault(group.getId(), List.of());
        List<TaskFormFieldVO> groupFieldVOs = new ArrayList<>();
        for (DynamicFormField field : groupFields) {
            TaskFormFieldVO fieldVO = toFieldVO(field, permissionMap, editable,
                    fieldInstanceValueMap.get(field.getId()), sourceFormId);
            groupFieldVOs.add(fieldVO);
        }
        groupVO.setFields(groupFieldVOs.isEmpty() ? null : groupFieldVOs);
        groups.add(groupVO);
    }
}
```

Helper `toFieldVO`:

```java
private TaskFormFieldVO toFieldVO(DynamicFormField field, Map<String, String> permissionMap,
        boolean editable, Object value, String sourceFormId) {
    TaskFormFieldVO vo = new TaskFormFieldVO();
    vo.setFieldKey(field.getFieldId());
    vo.setLabel(field.getTitle());
    vo.setType(field.getType() != null ? field.getType().name() : null);

    // 权限
    String permission = permissionMap.getOrDefault(field.getFieldId(), "VISIBLE");
    if (!editable) {
        permission = "READONLY"; // 非处理人强制只读
    }
    vo.setPermission(permission);

    vo.setValue(value);
    vo.setRequired("REQUIRED".equals(permission)
            || "1".equals(field.getRequired())); // 必填权限 或 模板自身必填
    vo.setOptions(field.getOptions());
    vo.setSourceFormId(sourceFormId);
    return vo;
}
```

Helper `buildInheritedForm`:

```java
private TaskFormInheritedVO buildInheritedForm(Integer processInstanceId,
        ProcessDefinitionForm globalBinding, Map<String, String> permissionMap,
        boolean editable) {
    ProcessInstanceForm globalRel = processInstanceFormMapper.selectOne(
            new LambdaQueryWrapper<ProcessInstanceForm>()
                    .eq(ProcessInstanceForm::getProcessInstanceId, processInstanceId)
                    .isNull(ProcessInstanceForm::getNodeId));

    TaskFormInheritedVO inherited = new TaskFormInheritedVO();
    inherited.setFormId(globalBinding.getFormId());
    inherited.setFormVersion(globalBinding.getFormVersion());
    inherited.setFormInstanceId(globalRel != null ? globalRel.getFormInstanceId() : null);

    List<TaskFormFieldVO> formFields = new ArrayList<>();
    List<TaskFormGroupVO> groups = new ArrayList<>();
    buildFormFieldsAndGroups(globalBinding.getFormId(), globalBinding.getFormVersion(),
            globalRel != null ? globalRel.getFormInstanceId() : null,
            permissionMap, editable, globalBinding.getFormId(), formFields, groups);
    inherited.setFormFields(formFields.isEmpty() ? null : formFields);
    inherited.setGroups(groups.isEmpty() ? null : groups);
    return inherited;
}
```

- [ ] **Step 5: Commit**

```bash
git add simple/src/main/java/com/cat/simple/process/service/ProcessFormService.java simple/src/main/java/com/cat/simple/process/service/impl/ProcessFormServiceImpl.java
git commit -m "feat: add ProcessFormService with resolveFormConfig, createFormInstance, writeFormData, buildTaskForm"
```

---

## Task 5: Update StartProcessCommand and StartContext

**Files:**
- Modify: `common/src/main/java/com/cat/common/entity/process/ProcessHandleParam.java` (already done in Task 3)
- Modify: `simple/src/main/java/com/cat/simple/config/flowable/hook/StartContext.java`
- Modify: `simple/src/main/java/com/cat/simple/config/flowable/command/StartProcessCommand.java`
- Modify: `simple/src/main/java/com/cat/simple/process/controller/ProcessInstanceController.java`
- Modify: `simple/src/main/java/com/cat/simple/process/service/ProcessInstanceService.java`
- Modify: `simple/src/main/java/com/cat/simple/process/service/impl/ProcessInstanceServiceImpl.java`

- [ ] **Step 1: Add formData to StartContext**

In `StartContext.java`, the `initialVariables` field already exists. We need to add a dedicated `formData` field:

```java
@Schema(description = "表单数据")
private Map<String, Object> formData;
```

Update the constructor or add a new one:

```java
public StartContext(Integer processDefinitionId, String title, String applicantId,
                    Map<String, Object> initialVariables, Map<String, Object> formData) {
    this.processDefinitionId = processDefinitionId;
    this.title = title;
    this.applicantId = applicantId;
    this.initialVariables = initialVariables;
    this.formData = formData;
}
```

- [ ] **Step 2: Update ProcessInstanceService interface**

Change `start` signature to include `formData`:

```java
ProcessInstance start(Integer processDefinitionId, String title, Map<String, Object> formData);
```

Change `saveDraft` signature to include `formData`:

```java
ProcessInstance saveDraft(Integer id, Integer processDefinitionId, String title, Map<String, Object> formData);
```

Add import: `import java.util.Map;`

- [ ] **Step 3: Create StartProcessParam DTO**

Because Spring doesn't allow mixing `@RequestParam` and `@RequestBody` on the same POST, create a request body DTO for start and saveDraft.

Create `common/src/main/java/com/cat/common/entity/process/StartProcessParam.java`:

```java
package com.cat.common.entity.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "发起流程参数")
public class StartProcessParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "自建流程定义id", required = true)
    private Integer processDefinitionId;

    @Schema(description = "流程标题")
    private String title;

    @Schema(description = "表单数据")
    private Map<String, Object> formData;
}
```

Create `common/src/main/java/com/cat/common/entity/process/SaveDraftParam.java`:

```java
package com.cat.common.entity.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "保存草稿参数")
public class SaveDraftParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "草稿流程实例id，传则更新，不传则新建")
    private Integer id;

    @Schema(description = "自建流程定义id", required = true)
    private Integer processDefinitionId;

    @Schema(description = "流程标题")
    private String title;

    @Schema(description = "表单数据")
    private Map<String, Object> formData;
}
```

- [ ] **Step 4: Update ProcessInstanceController**

Update `start` endpoint to use request body:

```java
@Operation(summary = "发起流程")
@RequestMapping(value = "/start", method = RequestMethod.POST)
public HttpResult<ProcessInstance> start(@RequestBody StartProcessParam param) {
    return HttpResult.back(processInstanceService.start(
            param.getProcessDefinitionId(), param.getTitle(), param.getFormData()));
}
```

Update `saveDraft` endpoint similarly:

```java
@Operation(summary = "保存草稿")
@RequestMapping(value = "/saveDraft", method = RequestMethod.POST)
public HttpResult<ProcessInstance> saveDraft(@RequestBody SaveDraftParam param) {
    return HttpResult.back(processInstanceService.saveDraft(
            param.getId(), param.getProcessDefinitionId(), param.getTitle(), param.getFormData()));
}
```

Add imports:
```java
import com.cat.common.entity.process.StartProcessParam;
import com.cat.common.entity.process.SaveDraftParam;
```

- [ ] **Step 5: Update StartProcessCommand**

Add `formData` field and update constructor:

```java
private final Map<String, Object> formData;

public StartProcessCommand(Integer processDefinitionId, String title, Map<String, Object> formData) {
    this.processDefinitionId = processDefinitionId;
    this.title = title;
    this.formData = formData;
}
```

Add `ProcessFormService` injection:

```java
@Resource private ProcessFormService processFormService;
```

Update `beforeHook` to pass formData:

```java
@Override
protected void beforeHook() {
    StartContext ctx = new StartContext(processDefinitionId, title, guard.getCurrentUserId(), null, formData);
    lifecycleHook.beforeStart(ctx);
}
```

Update `doExecute` to create form instance and write data after inserting ProcessInstance:

```java
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

    // 创建表单实例并写入数据
    processFormService.createFormInstanceIfNeeded(instance.getId(), definition.getId(), null);
    processFormService.writeFormData(instance.getId(), definition.getId(), null, formData, false);

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
```

- [ ] **Step 6: Update ProcessInstanceServiceImpl.start()**

```java
@Override
@Transactional(rollbackFor = Exception.class)
public ProcessInstance start(Integer processDefinitionId, String title, Map<String, Object> formData) {
    return commandBus.execute(new StartProcessCommand(processDefinitionId, title, formData));
}
```

- [ ] **Step 7: Commit**

```bash
git add common/src/main/java/com/cat/common/entity/process/StartProcessParam.java common/src/main/java/com/cat/common/entity/process/SaveDraftParam.java simple/src/main/java/com/cat/simple/config/flowable/hook/StartContext.java simple/src/main/java/com/cat/simple/config/flowable/command/StartProcessCommand.java simple/src/main/java/com/cat/simple/process/controller/ProcessInstanceController.java simple/src/main/java/com/cat/simple/process/service/ProcessInstanceService.java simple/src/main/java/com/cat/simple/process/service/impl/ProcessInstanceServiceImpl.java
git commit -m "feat: integrate form data into start flow"
```

---

## Task 6: Update saveDraft with form data

**Files:**
- Modify: `simple/src/main/java/com/cat/simple/process/service/impl/ProcessInstanceServiceImpl.java`

- [ ] **Step 1: Update saveDraft method**

```java
@Override
@Transactional(rollbackFor = Exception.class)
public ProcessInstance saveDraft(Integer id, Integer processDefinitionId,
                                  String title, Map<String, Object> formData) {
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

        // 创建表单实例（如需）并写入数据（草稿跳过必填校验）
        processFormService.createFormInstanceIfNeeded(id, definition.getId(), null);
        processFormService.writeFormData(id, definition.getId(), null, formData, true);

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

    // 创建表单实例（如需）并写入数据（草稿跳过必填校验）
    processFormService.createFormInstanceIfNeeded(instance.getId(), definition.getId(), null);
    processFormService.writeFormData(instance.getId(), definition.getId(), null, formData, true);

    return instance;
}
```

Add injection:

```java
@Resource
private ProcessFormService processFormService;
```

- [ ] **Step 2: Commit**

```bash
git add simple/src/main/java/com/cat/simple/process/service/impl/ProcessInstanceServiceImpl.java
git commit -m "feat: integrate form data into saveDraft"
```

---

## Task 7: Update PassTaskCommand with form validation

**Files:**
- Modify: `simple/src/main/java/com/cat/simple/config/flowable/command/PassTaskCommand.java`

- [ ] **Step 1: Add form data handling to PassTaskCommand**

```java
package com.cat.simple.config.flowable.command;

import com.cat.common.entity.process.ProcessHandleParam;
import com.cat.simple.config.flowable.hook.PassContext;
import com.cat.simple.process.service.ProcessFormService;
import jakarta.annotation.Resource;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;

public class PassTaskCommand extends ProcessCommand<Void> {

    @Resource private TaskService taskService;
    @Resource private ProcessFormService processFormService;

    private final ProcessHandleParam param;
    private Task task;

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
        this.task = guard.getTask(param.getTaskId());

        // 校验并写入表单数据
        if (param.getFormData() != null && !param.getFormData().isEmpty()) {
            ProcessInstance instance = guard.getInstance(param.getProcessInstanceId());
            processFormService.writeFormData(
                    param.getProcessInstanceId(),
                    instance.getProcessDefinitionId(),
                    task.getTaskDefinitionKey(),
                    param.getFormData(),
                    false); // pass 必须校验必填
        }

        taskService.complete(task.getId());
        return null;
    }

    @Override
    protected void record(Void result) {
        recorder.recordPass(param, task);
    }

    @Override
    protected void beforeHook() {
        PassContext ctx = new PassContext(param.getProcessInstanceId(), param.getTaskId(),
                param.getRemark(), param.getFormData());
        lifecycleHook.beforePass(ctx);
    }

    @Override
    protected void afterHook(Void result) {
        lifecycleHook.afterPass(guard.getInstance(param.getProcessInstanceId()));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add simple/src/main/java/com/cat/simple/config/flowable/command/PassTaskCommand.java
git commit -m "feat: add form data validation to PassTaskCommand"
```

---

## Task 8: Update ApprovalTaskCreateListener for node form creation

**Files:**
- Modify: `simple/src/main/java/com/cat/simple/config/flowable/listener/ApprovalTaskCreateListener.java`

- [ ] **Step 1: Add node form instance creation**

The listener runs for every task creation. We need to check if the task's node has a form binding and create the form instance if needed.

The challenge: `ApprovalTaskCreateListener` is a Flowable `TaskListener` that doesn't have access to our custom `ProcessInstance` ID directly — it gets `DelegateTask` from which we can get the Flowable process instance ID. We need to look up our `ProcessInstance` by the Flowable ID.

Add to `ApprovalTaskCreateListener`:

```java
@Resource
private com.cat.simple.process.mapper.ProcessInstanceMapper processInstanceMapper;

@Resource
private com.cat.simple.process.service.ProcessFormService processFormService;
```

Add a method called at the end of `notify()`, after the existing approval handler logic:

```java
@Override
public void notify(DelegateTask delegateTask) {
    // ... existing approval type handling code stays the same ...

    // 创建节点表单实例（如需）
    createNodeFormInstance(delegateTask);
}

private void createNodeFormInstance(DelegateTask delegateTask) {
    try {
        String flowableInstanceId = delegateTask.getProcessInstanceId();
        ProcessInstance instance = processInstanceMapper.selectOne(
                new LambdaQueryWrapper<ProcessInstance>()
                        .eq(ProcessInstance::getProcessInstanceId, flowableInstanceId));
        if (instance == null) {
            return;
        }
        String nodeId = delegateTask.getTaskDefinitionKey();
        processFormService.createFormInstanceIfNeeded(
                instance.getId(), instance.getProcessDefinitionId(), nodeId);
    } catch (Exception e) {
        log.error("创建节点表单实例失败, taskId={}", delegateTask.getId(), e);
    }
}
```

Add imports:

```java
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.process.ProcessInstance;
```

- [ ] **Step 2: Commit**

```bash
git add simple/src/main/java/com/cat/simple/config/flowable/listener/ApprovalTaskCreateListener.java
git commit -m "feat: create node form instance on task arrival in ApprovalTaskCreateListener"
```

---

## Task 9: Update info endpoint to return taskForm

**Files:**
- Modify: `simple/src/main/java/com/cat/simple/process/service/impl/ProcessInstanceServiceImpl.java`

- [ ] **Step 1: Add taskForm assembly to info()**

In the `info()` method, after the existing taskId/taskName assignment logic, add taskForm assembly:

```java
@Override
public ProcessInstance info(Integer id, String taskId) {
    ProcessInstance instance = processInstanceMapper.selectInfoById(id);
    if (instance == null) {
        return null;
    }
    List<ProcessHandleInfo> handleList =
            processHandleInfoMapper.selectDetailListByProcessInstanceId(instance.getId());
    instance.setProcessHandleInfoList(handleList);
    instance.setTimeline(buildTimeline(handleList, instance));

    if (StringUtils.hasText(taskId)) {
        Task task = guard.assertTaskExists(taskId);
        if (task.getProcessInstanceId().equals(instance.getProcessInstanceId())) {
            String userId = guard.getCurrentUserId();
            boolean isAssignee = userId.equals(task.getAssignee());
            boolean isCandidate = taskService.createTaskQuery()
                    .taskId(taskId)
                    .taskCandidateUser(userId)
                    .singleResult() != null;
            boolean editable = isAssignee || isCandidate;

            if (editable) {
                instance.setTaskId(taskId);
                instance.setTaskName(task.getName());
            }

            // 组装 taskForm
            TaskFormVO taskForm = processFormService.buildTaskForm(
                    id, instance.getProcessDefinitionId(),
                    task.getTaskDefinitionKey(), editable);
            instance.setTaskForm(taskForm);
        }
    }
    return instance;
}
```

Add import: `import com.cat.common.entity.process.TaskFormVO;`

- [ ] **Step 2: Commit**

```bash
git add simple/src/main/java/com/cat/simple/process/service/impl/ProcessInstanceServiceImpl.java
git commit -m "feat: return taskForm in processInstance info endpoint"
```

---

## Task 10: Compile and run tests

- [ ] **Step 1: Run full compile**

```bash
mvn clean install -DskipTests
```

Fix any compilation errors.

- [ ] **Step 2: Run existing tests**

```bash
cd simple && mvn test
```

Fix any test failures caused by the signature changes (start/saveDraft now take extra `formData` parameter).

- [ ] **Step 3: Commit any fixes**

```bash
git add -A
git commit -m "fix: resolve compilation and test issues from form integration"
```

---

## Task 11: Execute SQL and manual integration test

- [ ] **Step 1: Run the DDL**

Execute `sql/process_instance_form.sql` against the database.

- [ ] **Step 2: Start the application**

```bash
cd simple && mvn spring-boot:run
```

- [ ] **Step 3: Test the flow via Knife4j**

1. Create and publish a dynamic form
2. Create a process definition with globalFormBinding
3. Call `/processInstance/start` with formData
4. Call `/processInstance/info` with taskId — verify taskForm is returned
5. Call `/processInstance/pass` with formData — verify data is saved

- [ ] **Step 4: Commit any fixes**

```bash
git add -A
git commit -m "fix: resolve integration test issues"
```