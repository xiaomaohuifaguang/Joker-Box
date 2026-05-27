# Remote Option Source Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add first-version backend support for generic API-backed option sources on dynamic form option fields.

**Architecture:** Add a persisted `optionSource` JSON config to `DynamicFormField`, modeled by focused value objects in `common`. Backend validation treats `STATIC` or missing `optionSource` as the existing static `options` flow, while `API` mode validates only configuration and value/default-value structure without requesting remote APIs. Version-copy SQL must preserve `option_source` so publish/stop/draft lifecycle keeps the configuration.

**Tech Stack:** Java 21, Spring Boot 3.2, MyBatis-Plus, Fastjson2TypeHandler, MySQL, JUnit 5 / Spring Test.

---

## File Structure

**Create:**
- `common/src/main/java/com/cat/common/entity/dynamicForm/DynamicFormOptionSource.java` — field-level option source configuration.
- `common/src/main/java/com/cat/common/entity/dynamicForm/DynamicFormOptionMapping.java` — API response mapping configuration.
- `simple/src/main/resources/db/alter_dynamic_form_field_add_option_source.sql` — DDL migration for `cat_dynamic_form_field.option_source`.
- `simple/src/test/java/com/cat/simple/form/service/impl/DynamicFormServiceImplRemoteOptionSourceTest.java` — unit tests for deploy-time and submit-time validation.

**Modify:**
- `common/src/main/java/com/cat/common/entity/dynamicForm/DynamicFormField.java` — add `optionSource` with `Fastjson2TypeHandler`.
- `simple/src/main/resources/mapper/DynamicFormFieldMapper.xml` — copy `option_source` during form version copy.
- `simple/src/main/java/com/cat/simple/form/service/impl/DynamicFormServiceImpl.java` — add option source validation and API-mode value/default structure validation.

---

### Task 1: Add option source model and persistence

**Files:**
- Create: `common/src/main/java/com/cat/common/entity/dynamicForm/DynamicFormOptionSource.java`
- Create: `common/src/main/java/com/cat/common/entity/dynamicForm/DynamicFormOptionMapping.java`
- Modify: `common/src/main/java/com/cat/common/entity/dynamicForm/DynamicFormField.java`
- Create: `simple/src/main/resources/db/alter_dynamic_form_field_add_option_source.sql`
- Modify: `simple/src/main/resources/mapper/DynamicFormFieldMapper.xml`

- [ ] **Step 1: Create `DynamicFormOptionSource`**

```java
package com.cat.common.entity.dynamicForm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "DynamicFormOptionSource", description = "动态表单选项数据源配置")
public class DynamicFormOptionSource implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "选项来源类型：STATIC/API")
    private String type;

    @Schema(description = "API 地址，仅 API 模式使用")
    private String url;

    @Schema(description = "请求方式：GET/POST，仅 API 模式使用")
    private String method;

    @Schema(description = "静态请求参数")
    private Map<String, Object> params;

    @Schema(description = "API 响应映射配置")
    private DynamicFormOptionMapping mapping;
}
```

- [ ] **Step 2: Create `DynamicFormOptionMapping`**

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
@Schema(name = "DynamicFormOptionMapping", description = "动态表单远程选项映射配置")
public class DynamicFormOptionMapping implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "候选项数组路径，响应根数组使用 $")
    private String listPath;

    @Schema(description = "选项显示文本字段路径")
    private String labelPath;

    @Schema(description = "选项值字段路径")
    private String valuePath;

    @Schema(description = "子选项字段路径")
    private String childrenPath;
}
```

- [ ] **Step 3: Add `optionSource` to `DynamicFormField`**

In `common/src/main/java/com/cat/common/entity/dynamicForm/DynamicFormField.java`, insert immediately after `options`:

```java
    @Schema(description = "选项远程数据源配置")
    @TableField(typeHandler = Fastjson2TypeHandler.class)
    private DynamicFormOptionSource optionSource;
```

The surrounding block should become:

```java
    @Schema(description = "单选多选配置")
    @TableField(typeHandler = Fastjson2TypeHandler.class)
    private List<DynamicFormOption> options = new ArrayList<>();

    @Schema(description = "选项远程数据源配置")
    @TableField(typeHandler = Fastjson2TypeHandler.class)
    private DynamicFormOptionSource optionSource;

    @Schema(description = "最小长度")
    private Integer minLength;
```

- [ ] **Step 4: Add DDL migration**

Create `simple/src/main/resources/db/alter_dynamic_form_field_add_option_source.sql`:

```sql
ALTER TABLE `cat_dynamic_form_field`
ADD COLUMN `option_source` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '选项远程数据源配置' AFTER `options`;
```

- [ ] **Step 5: Update `copyVersion` SQL**

In `simple/src/main/resources/mapper/DynamicFormFieldMapper.xml`, change the insert/select lists from:

```xml
        (id, form_id, group_id, version, field_id, title, type, required, default_value, placeholder,
         options, columns, min_length, max_length, min, max, pattern, pattern_tips, span,
         deleted, create_by, create_time, update_time, sort)
      SELECT
        REPLACE(UUID(), '-', ''), form_id, group_id, #{targetVersion},
        field_id, title, type, required, default_value, placeholder,
        options, columns, min_length, max_length, min, max, pattern, pattern_tips, span,
        '0', create_by, NOW(), NOW(), sort
```

to:

```xml
        (id, form_id, group_id, version, field_id, title, type, required, default_value, placeholder,
         options, option_source, columns, min_length, max_length, min, max, pattern, pattern_tips, span,
         deleted, create_by, create_time, update_time, sort)
      SELECT
        REPLACE(UUID(), '-', ''), form_id, group_id, #{targetVersion},
        field_id, title, type, required, default_value, placeholder,
        options, option_source, columns, min_length, max_length, min, max, pattern, pattern_tips, span,
        '0', create_by, NOW(), NOW(), sort
```

- [ ] **Step 6: Compile common module**

Run: `mvn install -pl common -DskipTests`

Expected: `BUILD SUCCESS`

- [ ] **Step 7: Compile simple module**

Run: `mvn compile -pl simple -DskipTests`

Expected: `BUILD SUCCESS`

- [ ] **Step 8: Commit**

Only run this commit step if commits are authorized for the session.

```bash
git add common/src/main/java/com/cat/common/entity/dynamicForm/DynamicFormOptionSource.java \
  common/src/main/java/com/cat/common/entity/dynamicForm/DynamicFormOptionMapping.java \
  common/src/main/java/com/cat/common/entity/dynamicForm/DynamicFormField.java \
  simple/src/main/resources/db/alter_dynamic_form_field_add_option_source.sql \
  simple/src/main/resources/mapper/DynamicFormFieldMapper.xml
git commit -m "feat: add remote option source field config"
```

---

### Task 2: Add deploy-time validation tests for optionSource

**Files:**
- Create: `simple/src/test/java/com/cat/simple/form/service/impl/DynamicFormServiceImplRemoteOptionSourceTest.java`

- [ ] **Step 1: Write failing tests**

Create `simple/src/test/java/com/cat/simple/form/service/impl/DynamicFormServiceImplRemoteOptionSourceTest.java`:

```java
package com.cat.simple.form.service.impl;

import com.cat.common.entity.dynamicForm.DynamicFormField;
import com.cat.common.entity.dynamicForm.DynamicFormFieldType;
import com.cat.common.entity.dynamicForm.DynamicFormOption;
import com.cat.common.entity.dynamicForm.DynamicFormOptionMapping;
import com.cat.common.entity.dynamicForm.DynamicFormOptionSource;
import com.cat.common.entity.dynamicForm.DynamicFormLinkageRule;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicFormServiceImplRemoteOptionSourceTest {
    private final DynamicFormServiceImpl service = new DynamicFormServiceImpl();

    @Test
    void validateFieldAllowsApiSelectWithoutStaticOptions() {
        DynamicFormField field = baseField(DynamicFormFieldType.SELECT)
                .setOptionSource(apiSource())
                .setOptions(List.of());

        assertDoesNotThrow(() -> validateField(field));
    }

    @Test
    void validateFieldRejectsApiSourceOnUnsupportedFieldType() {
        DynamicFormField field = baseField(DynamicFormFieldType.INPUT)
                .setOptionSource(apiSource());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validateField(field));

        assertTrue(ex.getMessage().contains("不支持远程选项数据源"));
    }

    @Test
    void validateFieldRejectsAbsoluteApiUrl() {
        DynamicFormField field = baseField(DynamicFormFieldType.SELECT)
                .setOptionSource(apiSource().setUrl("https://example.com/options"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validateField(field));

        assertTrue(ex.getMessage().contains("url 必须是同源相对路径"));
    }

    @Test
    void validateFieldKeepsStaticSelectOptionsRequired() {
        DynamicFormField field = baseField(DynamicFormFieldType.SELECT)
                .setOptions(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validateField(field));

        assertTrue(ex.getMessage().contains("缺少选项"));
    }

    @Test
    void validateFieldApiCascaderDefaultOnlyChecksStructure() {
        DynamicFormField field = baseField(DynamicFormFieldType.CASCADER)
                .setOptionSource(apiSource().setMapping(apiMapping().setChildrenPath("children")))
                .setOptions(List.of())
                .setDefaultValue(List.of("unknown", "node"));

        assertDoesNotThrow(() -> validateField(field));
    }

    @Test
    void validateFormDataAllowsApiMultiSelectWithoutStaticOptions() {
        DynamicFormField field = baseField(DynamicFormFieldType.MULTISELECT)
                .setOptionSource(apiSource())
                .setOptions(List.of());

        assertDoesNotThrow(() -> validateFormData(List.of(field), Map.of("fieldA", List.of("x", "y"))));
    }

    @Test
    void validateFormDataRejectsApiMultiSelectNonListValue() {
        DynamicFormField field = baseField(DynamicFormFieldType.MULTISELECT)
                .setOptionSource(apiSource());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validateFormData(List.of(field), Map.of("fieldA", "x")));

        assertTrue(ex.getMessage().contains("值格式错误"));
    }

    @Test
    void validateFormDataRejectsApiCascaderNonListPath() {
        DynamicFormField field = baseField(DynamicFormFieldType.CASCADER)
                .setOptionSource(apiSource().setMapping(apiMapping().setChildrenPath("children")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validateFormData(List.of(field), Map.of("fieldA", "a,b")));

        assertTrue(ex.getMessage().contains("值格式错误"));
    }

    @Test
    void validateFormDataKeepsStaticMultiSelectOptionMembershipCheck() {
        DynamicFormField field = baseField(DynamicFormFieldType.MULTISELECT)
                .setOptions(List.of(new DynamicFormOption("A", "a")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validateFormData(List.of(field), Map.of("fieldA", List.of("b"))));

        assertTrue(ex.getMessage().contains("包含无效选项值"));
    }

    private void validateField(DynamicFormField field) {
        ReflectionTestUtils.invokeMethod(service, "validateField", field);
    }

    @SuppressWarnings("unchecked")
    private void validateFormData(List<DynamicFormField> fields, Map<String, Object> data) {
        ReflectionTestUtils.invokeMethod(service, "validateFormData", fields, data, List.<DynamicFormLinkageRule>of());
    }

    private DynamicFormField baseField(DynamicFormFieldType type) {
        return new DynamicFormField()
                .setFieldId("fieldA")
                .setTitle("测试字段")
                .setType(type)
                .setRequired("0")
                .setSpan(24);
    }

    private DynamicFormOptionSource apiSource() {
        return new DynamicFormOptionSource()
                .setType("API")
                .setUrl("/api/common/options")
                .setMethod("GET")
                .setParams(Map.of("enabled", true))
                .setMapping(apiMapping());
    }

    private DynamicFormOptionMapping apiMapping() {
        return new DynamicFormOptionMapping()
                .setListPath("data")
                .setLabelPath("name")
                .setValuePath("id");
    }
}
```

- [ ] **Step 2: Run tests and verify failure**

Run: `mvn -pl simple -Dtest=DynamicFormServiceImplRemoteOptionSourceTest test`

Expected before implementation: tests fail because API mode still requires static `options`, unsupported field types are not rejected, and API submit structure is not handled.

---

### Task 3: Implement deploy-time optionSource validation

**Files:**
- Modify: `simple/src/main/java/com/cat/simple/form/service/impl/DynamicFormServiceImpl.java`

- [ ] **Step 1: Add helper methods before `validateField`**

Insert after `validateFieldIdUniqueness(...)` and before `validateField(...)`:

```java
    private static final Set<DynamicFormFieldType> OPTION_SOURCE_FIELD_TYPES = Set.of(
            DynamicFormFieldType.SELECT,
            DynamicFormFieldType.MULTISELECT,
            DynamicFormFieldType.RADIO,
            DynamicFormFieldType.CHECKBOX,
            DynamicFormFieldType.CASCADER,
            DynamicFormFieldType.MULTICASCADER
    );

    private boolean isApiOptionSource(DynamicFormField field) {
        DynamicFormOptionSource optionSource = field.getOptionSource();
        return optionSource != null && "API".equalsIgnoreCase(optionSource.getType());
    }

    private void validateOptionSource(DynamicFormField field) {
        DynamicFormOptionSource optionSource = field.getOptionSource();
        if (optionSource == null || !StringUtils.hasText(optionSource.getType())) {
            return;
        }

        String type = optionSource.getType().trim().toUpperCase();
        if (!"STATIC".equals(type) && !"API".equals(type)) {
            throw new IllegalArgumentException("字段 \"" + field.getTitle() + "\" optionSource.type 只支持 STATIC 或 API");
        }
        if ("STATIC".equals(type)) {
            return;
        }
        if (!OPTION_SOURCE_FIELD_TYPES.contains(field.getType())) {
            throw new IllegalArgumentException("字段 \"" + field.getTitle() + "\" 不支持远程选项数据源");
        }
        if (!StringUtils.hasText(optionSource.getUrl())) {
            throw new IllegalArgumentException("字段 \"" + field.getTitle() + "\" optionSource.url 不能为空");
        }
        String url = optionSource.getUrl().trim();
        if (!url.startsWith("/") || url.startsWith("//")
                || url.startsWith("http://") || url.startsWith("https://")) {
            throw new IllegalArgumentException("字段 \"" + field.getTitle() + "\" optionSource.url 必须是同源相对路径");
        }
        if (!StringUtils.hasText(optionSource.getMethod())) {
            throw new IllegalArgumentException("字段 \"" + field.getTitle() + "\" optionSource.method 不能为空");
        }
        String method = optionSource.getMethod().trim().toUpperCase();
        if (!"GET".equals(method) && !"POST".equals(method)) {
            throw new IllegalArgumentException("字段 \"" + field.getTitle() + "\" optionSource.method 只支持 GET 或 POST");
        }
        DynamicFormOptionMapping mapping = optionSource.getMapping();
        if (mapping == null) {
            throw new IllegalArgumentException("字段 \"" + field.getTitle() + "\" optionSource.mapping 不能为空");
        }
        validateMappingPath(field.getTitle(), "mapping.listPath", mapping.getListPath(), true);
        validateMappingPath(field.getTitle(), "mapping.labelPath", mapping.getLabelPath(), false);
        validateMappingPath(field.getTitle(), "mapping.valuePath", mapping.getValuePath(), false);
        if (StringUtils.hasText(mapping.getChildrenPath())) {
            validateMappingPath(field.getTitle(), "mapping.childrenPath", mapping.getChildrenPath(), false);
        }
    }

    private void validateMappingPath(String title, String name, String path, boolean allowRootArray) {
        if (!StringUtils.hasText(path)) {
            throw new IllegalArgumentException("字段 \"" + title + "\" optionSource." + name + " 不能为空");
        }
        String trimmed = path.trim();
        if (allowRootArray && "$".equals(trimmed)) {
            return;
        }
        if (!trimmed.matches("^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)*$")) {
            throw new IllegalArgumentException("字段 \"" + title + "\" optionSource." + name + " 只支持简单点路径");
        }
    }

    private boolean isScalarOptionValue(Object value) {
        return value instanceof String || value instanceof Number;
    }

    private void validateApiOptionValueStructure(DynamicFormField field, Object value, boolean defaultValue) {
        if (value == null) {
            return;
        }
        String label = defaultValue ? "默认值" : "值";
        switch (field.getType()) {
            case SELECT, RADIO -> {
                if (!isScalarOptionValue(value)) {
                    throw new IllegalArgumentException("字段 \"" + field.getTitle() + "\" " + label + "格式错误，应为 string 或 number");
                }
            }
            case MULTISELECT, CHECKBOX -> {
                if (!(value instanceof List<?> arr)) {
                    throw new IllegalArgumentException("字段 \"" + field.getTitle() + "\" " + label + "格式错误，应为数组");
                }
                for (Object item : arr) {
                    if (!isScalarOptionValue(item)) {
                        throw new IllegalArgumentException("字段 \"" + field.getTitle() + "\" " + label + "数组项必须是 string 或 number");
                    }
                }
            }
            case CASCADER -> {
                if (!(value instanceof List<?> path)) {
                    throw new IllegalArgumentException("字段 \"" + field.getTitle() + "\" " + label + "格式错误，应为路径数组");
                }
                validateApiCascaderPathStructure(field.getTitle(), path, label);
            }
            case MULTICASCADER -> {
                if (!(value instanceof List<?> arr)) {
                    throw new IllegalArgumentException("字段 \"" + field.getTitle() + "\" " + label + "格式错误，应为路径数组的数组");
                }
                for (Object item : arr) {
                    if (!(item instanceof List<?> path)) {
                        throw new IllegalArgumentException("字段 \"" + field.getTitle() + "\" " + label + "格式错误，每条选中项应为路径数组");
                    }
                    validateApiCascaderPathStructure(field.getTitle(), path, label);
                }
            }
            default -> { }
        }
    }

    private void validateApiCascaderPathStructure(String title, List<?> path, String label) {
        if (path.isEmpty()) {
            throw new IllegalArgumentException("字段 \"" + title + "\" " + label + "路径不能为空");
        }
        for (Object node : path) {
            if (!isScalarOptionValue(node)) {
                throw new IllegalArgumentException("字段 \"" + title + "\" " + label + "路径节点必须是 string 或 number");
            }
        }
    }
```

- [ ] **Step 2: Call optionSource validation near the top of `validateField`**

In `validateField(DynamicFormField field)`, after the type null check and before span validation, insert:

```java
        validateOptionSource(field);
        boolean apiOptionSource = isApiOptionSource(field);
```

- [ ] **Step 3: Update `SELECT, RADIO` deploy branch**

Replace the whole `case SELECT, RADIO -> { ... }` block with:

```java
            case SELECT, RADIO -> {
                if (apiOptionSource) {
                    validateApiOptionValueStructure(field, field.getDefaultValue(), true);
                    break;
                }
                if (CollectionUtils.isEmpty(field.getOptions())) {
                    throw new IllegalArgumentException("字段 \"" + title + "\" 缺少选项");
                }
                validateOptions(field.getOptions(), title);
                if (StringUtils.hasText(defaultValueStr)) {
                    boolean match = field.getOptions().stream()
                            .anyMatch(o -> defaultValueStr.equals(o.getValue()));
                    if (!match) {
                        throw new IllegalArgumentException("字段 \"" + title + "\" 默认值不在选项列表中");
                    }
                }
            }
```

- [ ] **Step 4: Update `MULTISELECT, CHECKBOX` deploy branch**

At the start of the `case MULTISELECT, CHECKBOX -> {` block, insert:

```java
                if (apiOptionSource) {
                    validateApiOptionValueStructure(field, field.getDefaultValue(), true);
                    if (field.getMin() != null && field.getMin() < 0) {
                        throw new IllegalArgumentException("字段 \"" + title + "\" 最少勾选数不能为空负数");
                    }
                    if (field.getMax() != null && field.getMax() < 1) {
                        throw new IllegalArgumentException("字段 \"" + title + "\" 最多勾选数不能小于1");
                    }
                    if (field.getMin() != null && field.getMax() != null && field.getMax() < field.getMin()) {
                        throw new IllegalArgumentException("字段 \"" + title + "\" 最多勾选数不能小于最少勾选数");
                    }
                    break;
                }
```

Then leave the existing static `options` validation in place.

- [ ] **Step 5: Update `CASCADER` deploy branch**

At the start of the `case CASCADER -> {` block, insert:

```java
                if (apiOptionSource) {
                    validateApiOptionValueStructure(field, field.getDefaultValue(), true);
                    validateCascaderProps(field);
                    break;
                }
```

Then leave existing static `options` tree validation in place.

- [ ] **Step 6: Update `MULTICASCADER` deploy branch**

At the start of the `case MULTICASCADER -> {` block, insert:

```java
                if (apiOptionSource) {
                    validateApiOptionValueStructure(field, field.getDefaultValue(), true);
                    validateCascaderProps(field);
                    break;
                }
```

Then leave existing static `options` tree validation in place.

- [ ] **Step 7: Run focused test**

Run: `mvn -pl simple -Dtest=DynamicFormServiceImplRemoteOptionSourceTest test`

Expected: deploy-time tests pass; submit-time tests may still fail until Task 4.

- [ ] **Step 8: Compile modules**

Run: `mvn install -pl common -DskipTests && mvn compile -pl simple -DskipTests`

Expected: both commands report `BUILD SUCCESS`.

- [ ] **Step 9: Commit**

Only run this commit step if commits are authorized for the session.

```bash
git add simple/src/main/java/com/cat/simple/form/service/impl/DynamicFormServiceImpl.java \
  simple/src/test/java/com/cat/simple/form/service/impl/DynamicFormServiceImplRemoteOptionSourceTest.java
git commit -m "feat: validate remote option source config on deploy"
```

---

### Task 4: Implement submit-time API option structure validation

**Files:**
- Modify: `simple/src/main/java/com/cat/simple/form/service/impl/DynamicFormServiceImpl.java`
- Test: `simple/src/test/java/com/cat/simple/form/service/impl/DynamicFormServiceImplRemoteOptionSourceTest.java`

- [ ] **Step 1: Update `MULTISELECT, CHECKBOX` submit branch**

Replace the current `case MULTISELECT, CHECKBOX -> { ... }` block in `validateFormData(...)` with:

```java
                case MULTISELECT, CHECKBOX -> {
                    if (isApiOptionSource(field) && value != null && !(value instanceof List<?>)) {
                        throw new IllegalArgumentException(field.getTitle() + " 值格式错误，应为数组");
                    }
                    List<?> arr = extractList(value);
                    if ("1".equals(field.getRequired()) && isEmptyList(arr)) {
                        throw new IllegalArgumentException(field.getTitle() + " 必填");
                    }
                    if (isEmptyList(arr)) {
                        continue;
                    }
                    if (field.getMin() != null && arr.size() < field.getMin()) {
                        throw new IllegalArgumentException(
                                field.getTitle() + " 至少选择 " + field.getMin() + " 项");
                    }
                    if (field.getMax() != null && arr.size() > field.getMax()) {
                        throw new IllegalArgumentException(
                                field.getTitle() + " 最多选择 " + field.getMax() + " 项");
                    }
                    if (isApiOptionSource(field)) {
                        for (Object item : arr) {
                            if (!isScalarOptionValue(item)) {
                                throw new IllegalArgumentException(field.getTitle() + " 选项值必须是 string 或 number");
                            }
                        }
                        continue;
                    }
                    Set<String> optionValues = extractOptionValues(field.getOptions());
                    for (Object item : arr) {
                        if (!optionValues.contains(String.valueOf(item))) {
                            throw new IllegalArgumentException(
                                    field.getTitle() + " 包含无效选项值: " + item);
                        }
                    }
                }
```

- [ ] **Step 2: Update `MULTICASCADER` submit branch**

Replace the current `case MULTICASCADER -> { ... }` block with:

```java
                case MULTICASCADER -> {
                    if (isApiOptionSource(field) && value != null && !(value instanceof List<?>)) {
                        throw new IllegalArgumentException(field.getTitle() + " 值格式错误，应为路径数组的数组");
                    }
                    List<?> arr = extractList(value);
                    if ("1".equals(field.getRequired()) && isEmptyList(arr)) {
                        throw new IllegalArgumentException(field.getTitle() + " 必填");
                    }
                    if (isEmptyList(arr)) {
                        continue;
                    }
                    if (field.getMin() != null && arr.size() < field.getMin()) {
                        throw new IllegalArgumentException(
                                field.getTitle() + " 至少选择 " + field.getMin() + " 项");
                    }
                    if (field.getMax() != null && arr.size() > field.getMax()) {
                        throw new IllegalArgumentException(
                                field.getTitle() + " 最多选择 " + field.getMax() + " 项");
                    }
                    for (Object item : arr) {
                        if (item instanceof List<?> path) {
                            if (isApiOptionSource(field)) {
                                validateApiCascaderPathStructure(field.getTitle(), path, "值");
                            } else {
                                validateCascaderPath(field.getOptions(), path, field.getTitle());
                            }
                        } else {
                            throw new IllegalArgumentException(
                                    field.getTitle() + " 值格式错误，每条选中项应为路径数组");
                        }
                    }
                }
```

- [ ] **Step 3: Update `CASCADER` submit branch**

Replace the current `case CASCADER -> { ... }` block with:

```java
                case CASCADER -> {
                    if (isApiOptionSource(field) && value != null && !(value instanceof List<?>)) {
                        throw new IllegalArgumentException(field.getTitle() + " 值格式错误，应为路径数组");
                    }
                    List<?> path = extractList(value);
                    if ("1".equals(field.getRequired()) && isEmptyList(path)) {
                        throw new IllegalArgumentException(field.getTitle() + " 必填");
                    }
                    if (!isEmptyList(path)) {
                        if (isApiOptionSource(field)) {
                            validateApiCascaderPathStructure(field.getTitle(), path, "值");
                        } else {
                            validateCascaderPath(field.getOptions(), path, field.getTitle());
                        }
                    }
                }
```

- [ ] **Step 4: Add explicit `SELECT, RADIO` submit branch**

Insert this block before `case MULTISELECT, CHECKBOX -> {`:

```java
                case SELECT, RADIO -> {
                    if (value == null || !StringUtils.hasText(String.valueOf(value))) {
                        if ("1".equals(field.getRequired())) {
                            throw new IllegalArgumentException(field.getTitle() + " 必填");
                        }
                        continue;
                    }
                    if (!isScalarOptionValue(value)) {
                        throw new IllegalArgumentException(field.getTitle() + " 值格式错误，应为 string 或 number");
                    }
                    if (!isApiOptionSource(field)) {
                        Set<String> optionValues = extractOptionValues(field.getOptions());
                        if (!optionValues.contains(String.valueOf(value))) {
                            throw new IllegalArgumentException(field.getTitle() + " 包含无效选项值: " + value);
                        }
                    }
                }
```

- [ ] **Step 5: Run focused tests**

Run: `mvn -pl simple -Dtest=DynamicFormServiceImplRemoteOptionSourceTest test`

Expected: all tests in `DynamicFormServiceImplRemoteOptionSourceTest` pass.

- [ ] **Step 6: Compile modules**

Run: `mvn install -pl common -DskipTests && mvn compile -pl simple -DskipTests`

Expected: both commands report `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

Only run this commit step if commits are authorized for the session.

```bash
git add simple/src/main/java/com/cat/simple/form/service/impl/DynamicFormServiceImpl.java \
  simple/src/test/java/com/cat/simple/form/service/impl/DynamicFormServiceImplRemoteOptionSourceTest.java
git commit -m "feat: validate remote option values by structure"
```

---

### Task 5: Final verification and documentation alignment

**Files:**
- Verify: `docs/dynamic-form-remote-option-source-design.md`
- Verify: `docs/dynamic-form-remote-option-source-questions-answer.md`
- Verify: all files changed by Tasks 1-4

- [ ] **Step 1: Run focused remote option tests**

Run: `mvn -pl simple -Dtest=DynamicFormServiceImplRemoteOptionSourceTest test`

Expected: `BUILD SUCCESS`, with all tests passing.

- [ ] **Step 2: Run common install**

Run: `mvn install -pl common -DskipTests`

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run simple compile**

Run: `mvn compile -pl simple -DskipTests`

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Inspect changed files**

Run: `git status --short`

Expected changed files include:

```text
common/src/main/java/com/cat/common/entity/dynamicForm/DynamicFormOptionSource.java
common/src/main/java/com/cat/common/entity/dynamicForm/DynamicFormOptionMapping.java
common/src/main/java/com/cat/common/entity/dynamicForm/DynamicFormField.java
simple/src/main/java/com/cat/simple/form/service/impl/DynamicFormServiceImpl.java
simple/src/main/resources/db/alter_dynamic_form_field_add_option_source.sql
simple/src/main/resources/mapper/DynamicFormFieldMapper.xml
simple/src/test/java/com/cat/simple/form/service/impl/DynamicFormServiceImplRemoteOptionSourceTest.java
docs/dynamic-form-remote-option-source-design.md
docs/dynamic-form-remote-option-source-questions-answer.md
docs/superpowers/plans/2026-05-24-remote-option-source.md
```

- [ ] **Step 5: Self-review spec coverage**

Verify these requirements are represented in code or docs:

- `optionSource` persists as `option_source` and is copied by `copyVersion`.
- `optionSource` JSON uses camelCase at API level.
- Missing `optionSource` behaves as `STATIC`.
- `API` is allowed only on `SELECT`, `MULTISELECT`, `RADIO`, `CHECKBOX`, `CASCADER`, `MULTICASCADER`.
- `API` mode validates `url`, `method`, and `mapping` without remote requests.
- `headers` is not modeled.
- `params` is static config only.
- `mapping` supports dot paths and `$` for root array.
- `API` default values are structure-only validated.
- `API` submitted values are structure-only validated.
- `STATIC` validation still requires `options` and validates option membership.
- Runtime cache and `OPTION` filtering behavior are documented only; backend does not implement frontend runtime cache.

- [ ] **Step 6: Commit final docs/plan if authorized**

Only run this commit step if commits are authorized for the session.

```bash
git add docs/dynamic-form-remote-option-source-design.md \
  docs/dynamic-form-remote-option-source-questions-answer.md \
  docs/superpowers/plans/2026-05-24-remote-option-source.md
git commit -m "docs: document remote option source implementation plan"
```

---

## Self-Review Checklist

**1. Spec coverage:**
- Data model: `DynamicFormOptionSource`, `DynamicFormOptionMapping`, `DynamicFormField.optionSource`, DDL, mapper copyVersion → Task 1.
- Deploy validation: supported field types, URL/method/mapping validation, static fallback, API default structure validation → Task 3.
- Submit validation: API structure-only behavior, static option membership behavior → Task 4.
- Frontend runtime cache and OPTION recursive filtering semantics → documented in design docs, not implemented in backend → Task 5.

**2. Placeholder scan:**
- No TBD/TODO/placeholder implementation steps.
- Each code change step includes concrete code or exact replacement text.

**3. Type consistency:**
- Java field name is `optionSource`; DB column is `option_source`.
- `DynamicFormOptionSource.type` is a string accepting `STATIC`/`API`.
- `DynamicFormOptionSource.mapping` uses `DynamicFormOptionMapping`.
- Tests call existing private methods via `ReflectionTestUtils`.
- Submit/default structure helpers are shared to keep API mode behavior consistent.
