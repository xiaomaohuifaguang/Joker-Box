# TABLE 字段类型实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在动态表单系统中新增 TABLE 字段类型，支持可增删行的动态表格。

**Architecture:** 新增 DynamicFormTableColumn 值对象存储列定义，DynamicFormField 新增 columns 字段（Fastjson2TypeHandler），复用现有 min/max/required 做行约束校验，联动与 UPLOAD 保持一致（仅通用动作）。

**Tech Stack:** Spring Boot + MyBatis-Plus + MySQL

---

### Task 1: 新增 DynamicFormTableColumn 值对象 + DynamicFormFieldType.TABLE 枚举

**Files:**
- Create: `common/src/main/java/com/cat/common/entity/dynamicForm/DynamicFormTableColumn.java`
- Modify: `common/src/main/java/com/cat/common/entity/dynamicForm/DynamicFormFieldType.java`

- [ ] **Step 1: 创建 DynamicFormTableColumn**

```java
package com.cat.common.entity.dynamicForm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "DynamicFormTableColumn", description = "动态表格列定义")
public class DynamicFormTableColumn implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "列标识")
    private String key;

    @Schema(description = "列标题")
    private String title;
}
```

- [ ] **Step 2: DynamicFormFieldType 新增 TABLE**

在 `DynamicFormFieldType.java` 的 `DATERANGE` 后追加：

```java
    TABLE
```

- [ ] **Step 3: 编译 common 模块**

Run: `mvn install -pl common -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add common/src/main/java/com/cat/common/entity/dynamicForm/DynamicFormTableColumn.java common/src/main/java/com/cat/common/entity/dynamicForm/DynamicFormFieldType.java
git commit -m "feat: add DynamicFormTableColumn entity and TABLE enum value"
```

---

### Task 2: DynamicFormField 新增 columns 字段 + 数据库 DDL

**Files:**
- Modify: `common/src/main/java/com/cat/common/entity/dynamicForm/DynamicFormField.java`

- [ ] **Step 1: DynamicFormField 新增 columns 字段**

在 `props` 字段之前（约 line 118）插入：

```java
    @Schema(description = "动态表格列定义（仅 TABLE 类型使用）")
    @TableField(typeHandler = Fastjson2TypeHandler.class)
    private List<DynamicFormTableColumn> columns;
```

需在文件顶部确认 `import com.cat.common.entity.dynamicForm.DynamicFormTableColumn;` 存在（同包内无需显式 import）。

- [ ] **Step 2: 编译 common 模块**

Run: `mvn install -pl common -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: 执行数据库 DDL**

```sql
ALTER TABLE cat_dynamic_form_field ADD columns TEXT COMMENT '动态表格列定义（仅TABLE类型）';
```

由用户手动在数据库执行，或记录到 SQL 变更脚本。

- [ ] **Step 4: Commit**

```bash
git add common/src/main/java/com/cat/common/entity/dynamicForm/DynamicFormField.java
git commit -m "feat: add columns field to DynamicFormField for TABLE type"
```

---

### Task 3: 发布前校验 — validateField TABLE 分支

**Files:**
- Modify: `simple/src/main/java/com/cat/simple/form/service/impl/DynamicFormServiceImpl.java`

- [ ] **Step 1: 在 validateField() 的 switch(type) 中新增 TABLE 分支**

在 `case UPLOAD` 之前（约 line 1267）插入：

```java
            case TABLE -> {
                if (CollectionUtils.isEmpty(field.getColumns())) {
                    throw new IllegalArgumentException("字段 \"" + title + "\" 缺少列定义");
                }
                Set<String> colKeys = new HashSet<>();
                for (DynamicFormTableColumn col : field.getColumns()) {
                    if (!StringUtils.hasText(col.getKey())) {
                        throw new IllegalArgumentException("字段 \"" + title + "\" 列标识不能为空");
                    }
                    if (!col.getKey().matches("^[a-zA-Z][a-zA-Z0-9_]{0,31}$")) {
                        throw new IllegalArgumentException(
                                "字段 \"" + title + "\" 列标识格式错误：以字母开头，仅含字母数字下划线，最长32字符");
                    }
                    if (colKeys.contains(col.getKey())) {
                        throw new IllegalArgumentException(
                                "字段 \"" + title + "\" 列标识重复: " + col.getKey());
                    }
                    colKeys.add(col.getKey());
                    if (!StringUtils.hasText(col.getTitle()) || col.getTitle().trim().isEmpty()) {
                        throw new IllegalArgumentException("字段 \"" + title + "\" 列标题不能为空");
                    }
                    if (col.getTitle().trim().length() > 32) {
                        throw new IllegalArgumentException(
                                "字段 \"" + title + "\" 列标题长度不能超过32字符");
                    }
                }
                if (field.getMin() != null && field.getMin() < 0) {
                    throw new IllegalArgumentException("字段 \"" + title + "\" 最少行数不能为负数");
                }
                if (field.getMax() != null && field.getMax() < 1) {
                    throw new IllegalArgumentException("字段 \"" + title + "\" 最多行数不能小于1");
                }
                if (field.getMin() != null && field.getMax() != null && field.getMax() < field.getMin()) {
                    throw new IllegalArgumentException("字段 \"" + title + "\" 最多行数不能小于最少行数");
                }
                if (field.getDefaultValue() != null) {
                    List<?> rows = extractList(field.getDefaultValue());
                    for (Object row : rows) {
                        if (!(row instanceof Map<?, ?> map)) {
                            throw new IllegalArgumentException(
                                    "字段 \"" + title + "\" 默认值每行必须是对象");
                        }
                        Set<String> colKeySet = colKeys;
                        for (Object k : map.keySet()) {
                            if (!colKeySet.contains(String.valueOf(k))) {
                                throw new IllegalArgumentException(
                                        "字段 \"" + title + "\" 默认值包含未定义的列: " + k);
                            }
                        }
                    }
                }
            }
```

- [ ] **Step 2: 确保 import 存在**

在 `DynamicFormServiceImpl.java` 顶部确认有 `import com.cat.common.entity.dynamicForm.DynamicFormTableColumn;`。由于已有 `import com.cat.common.entity.dynamicForm.*;`，同包类自动覆盖。

- [ ] **Step 3: 编译 simple 模块**

Run: `mvn compile -pl simple -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add simple/src/main/java/com/cat/simple/form/service/impl/DynamicFormServiceImpl.java
git commit -m "feat: add TABLE field deploy validation in validateField"
```

---

### Task 4: 提交校验 — validateFormData TABLE 分支

**Files:**
- Modify: `simple/src/main/java/com/cat/simple/form/service/impl/DynamicFormServiceImpl.java`

- [ ] **Step 1: 在 validateFormData() 的 switch 中新增 TABLE 分支**

在 `case UPLOAD` 之前（约 line 818）插入：

```java
                case TABLE -> {
                    List<?> arr = extractList(value);
                    if ("1".equals(field.getRequired()) && isEmptyList(arr)) {
                        throw new IllegalArgumentException(field.getTitle() + " 必填");
                    }
                    if (field.getMin() != null && !isEmptyList(arr) && arr.size() < field.getMin()) {
                        throw new IllegalArgumentException(
                                field.getTitle() + " 至少需要 " + field.getMin() + " 行数据");
                    }
                    if (field.getMax() != null && !isEmptyList(arr) && arr.size() > field.getMax()) {
                        throw new IllegalArgumentException(
                                field.getTitle() + " 最多允许 " + field.getMax() + " 行数据");
                    }
                    if (isEmptyList(arr)) {
                        continue;
                    }
                    if (!CollectionUtils.isEmpty(field.getColumns())) {
                        Set<String> colKeys = field.getColumns().stream()
                                .map(DynamicFormTableColumn::getKey)
                                .collect(Collectors.toSet());
                        for (Object row : arr) {
                            if (!(row instanceof Map<?, ?> map)) {
                                throw new IllegalArgumentException(
                                        field.getTitle() + " 每行数据必须是对象");
                            }
                            for (Object k : map.keySet()) {
                                if (!colKeys.contains(String.valueOf(k))) {
                                    throw new IllegalArgumentException(
                                            field.getTitle() + " 包含未定义的列: " + k);
                                }
                            }
                        }
                    }
                }
```

注意：required=1 且 min > 0 时，min 校验已经涵盖了 required 的语义（行数 >= min > 0），不会重复报错。如果 arr 为空且 required=1，第一个检查就会抛异常，不会走到 min 检查。

- [ ] **Step 2: 编译 simple 模块**

Run: `mvn compile -pl simple -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add simple/src/main/java/com/cat/simple/form/service/impl/DynamicFormServiceImpl.java
git commit -m "feat: add TABLE field submit validation in validateFormData"
```

---

### Task 5: 联动兼容性 — validateActionCompatibility TABLE 支持

**Files:**
- Modify: `simple/src/main/java/com/cat/simple/form/service/impl/DynamicFormServiceImpl.java`

- [ ] **Step 1: 在 validateActionCompatibility() 中新增 TABLE 分支**

在 `if (fieldType == DynamicFormFieldType.UPLOAD)` 之后添加 TABLE 分支，与 UPLOAD 共享 commonActions：

```java
            if (fieldType == DynamicFormFieldType.UPLOAD
                    || fieldType == DynamicFormFieldType.TABLE) {
                validActions = new HashSet<>(commonActions);
            } else if (fieldType == DynamicFormFieldType.INPUT || fieldType == DynamicFormFieldType.TEXTAREA) {
```

即将原来的 `if (fieldType == DynamicFormFieldType.UPLOAD)` 改为 `if (fieldType == DynamicFormFieldType.UPLOAD || fieldType == DynamicFormFieldType.TABLE)`。

- [ ] **Step 2: 编译 simple 模块**

Run: `mvn compile -pl simple -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add simple/src/main/java/com/cat/simple/form/service/impl/DynamicFormServiceImpl.java
git commit -m "feat: add TABLE to validateActionCompatibility with common actions only"
```

---

### Task 6: 全量编译验证

**Files:** 无变更，纯验证

- [ ] **Step 1: 全量编译**

Run: `mvn install -pl common -DskipTests && mvn compile -pl simple -DskipTests`
Expected: 两个模块均 BUILD SUCCESS

- [ ] **Step 2: 确认所有改动文件**

Run: `git diff --stat HEAD~5`
Expected: 显示 5 个 commit 涉及的文件变更

---

## Self-Review Checklist

**1. Spec coverage:**
- 数据模型（DynamicFormTableColumn, TABLE 枚举, columns 字段, DDL）→ Task 1 + 2 ✓
- 发布前校验（columns 非空, key 格式/唯一, title, min/max, defaultValue）→ Task 3 ✓
- 提交校验（required, min/max 行数, 行数据格式, key 合法性）→ Task 4 ✓
- 联动支持（触发: EMPTY/NOT_EMPTY 自然支持; 目标: commonActions）→ Task 5 ✓
- 前端交互约定 → 无后端代码变更，已在设计文档中 ✓

**2. Placeholder scan:** 无 TBD/TODO/占位符 ✓

**3. Type consistency:**
- `DynamicFormTableColumn` 的 `getKey()` / `getTitle()` 在 Task 3 和 Task 4 中一致使用 ✓
- `DynamicFormFieldType.TABLE` 在 Task 1 定义，Task 5 引用 ✓
- `extractList()` 在 Task 3/4 中复用同一辅助方法 ✓
- `Fastjson2TypeHandler` 与 DynamicFormOption 的 options 字段一致 ✓