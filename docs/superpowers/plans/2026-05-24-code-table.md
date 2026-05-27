# Code Table Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a global code-table module that manages flat/tree option dictionaries and exposes a standard options API for dynamic form `optionSource`.

**Architecture:** Add `common` entity/parameter/DTO classes mapped to the existing `cat_code_table` and `cat_code_item` MySQL tables. Add a `simple` codeTable module with MyBatis-Plus mappers, one service aggregating code-table and code-item rules, and two controllers for CRUD and options queries. Dynamic forms continue to use `optionSource.type = API` with `/api/code-table/options`, so no dynamic-form model changes are needed.

**Tech Stack:** Java 21, Spring Boot 3.2, MyBatis-Plus, MySQL, Lombok, Spring Web, JUnit 5 / Mockito.

---

## File Structure

### Create

- `common/src/main/java/com/cat/common/entity/codeTable/CodeTable.java` — entity for `cat_code_table`.
- `common/src/main/java/com/cat/common/entity/codeTable/CodeItem.java` — entity for `cat_code_item`.
- `common/src/main/java/com/cat/common/entity/codeTable/CodeTablePageParam.java` — page query input for code tables.
- `common/src/main/java/com/cat/common/entity/codeTable/CodeItemQueryParam.java` — list/tree query input for code items.
- `common/src/main/java/com/cat/common/entity/codeTable/CodeOption.java` — options API response node.
- `simple/src/main/java/com/cat/simple/codeTable/mapper/CodeTableMapper.java` — MyBatis-Plus mapper for code tables.
- `simple/src/main/java/com/cat/simple/codeTable/mapper/CodeItemMapper.java` — MyBatis-Plus mapper for code items.
- `simple/src/main/java/com/cat/simple/codeTable/service/CodeTableService.java` — service interface for both code tables and items.
- `simple/src/main/java/com/cat/simple/codeTable/service/impl/CodeTableServiceImpl.java` — validation, CRUD, tree building, and options query implementation.
- `simple/src/main/java/com/cat/simple/codeTable/controller/CodeTableController.java` — code-table CRUD and `/api/code-table/options`.
- `simple/src/main/java/com/cat/simple/codeTable/controller/CodeItemController.java` — code-item CRUD and tree/list queries.
- `simple/src/test/java/com/cat/simple/codeTable/service/impl/CodeTableServiceImplTest.java` — focused service tests with mocked mappers.

### Modify

No existing source files should be modified for this feature.

### Database

The user has already created these MySQL tables:

- `cat_code_table`
- `cat_code_item`

Do not add a migration file unless the user asks for one.

---

## Task 1: Add common code-table model classes

**Files:**
- Create: `common/src/main/java/com/cat/common/entity/codeTable/CodeTable.java`
- Create: `common/src/main/java/com/cat/common/entity/codeTable/CodeItem.java`
- Create: `common/src/main/java/com/cat/common/entity/codeTable/CodeTablePageParam.java`
- Create: `common/src/main/java/com/cat/common/entity/codeTable/CodeItemQueryParam.java`
- Create: `common/src/main/java/com/cat/common/entity/codeTable/CodeOption.java`

- [ ] **Step 1: Create `CodeTable.java`**

```java
package com.cat.common.entity.codeTable;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_code_table")
@Schema(name = "CodeTable", description = "码表")
public class CodeTable implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_UUID)
    @Schema(description = "主键")
    private String id;

    @Schema(description = "码表编码，全局唯一")
    private String code;

    @Schema(description = "码表名称")
    private String name;

    @Schema(description = "是否树形：0否 1是")
    private String tree = "0";

    @Schema(description = "状态：0停用 1启用")
    private String status = "1";

    @Schema(description = "备注")
    private String remark;

    @TableLogic
    @Schema(description = "逻辑删除：0未删除 1已删除")
    private String deleted = "0";

    @Schema(description = "创建人")
    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
```

- [ ] **Step 2: Create `CodeItem.java`**

```java
package com.cat.common.entity.codeTable;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_code_item")
@Schema(name = "CodeItem", description = "码项")
public class CodeItem implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_UUID)
    @Schema(description = "主键")
    private String id;

    @Schema(description = "码表ID")
    private String tableId;

    @Schema(description = "父级码项ID")
    private String parentId;

    @Schema(description = "显示文本")
    private String label;

    @Schema(description = "提交值")
    private String value;

    @Schema(description = "排序")
    private Integer sort = 0;

    @Schema(description = "状态：0停用 1启用")
    private String status = "1";

    @Schema(description = "备注")
    private String remark;

    @TableLogic
    @Schema(description = "逻辑删除：0未删除 1已删除")
    private String deleted = "0";

    @Schema(description = "创建人")
    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
```

- [ ] **Step 3: Create `CodeTablePageParam.java`**

```java
package com.cat.common.entity.codeTable;

import com.cat.common.entity.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "CodeTablePageParam", description = "码表分页查询参数")
public class CodeTablePageParam extends PageParam {
    @Schema(description = "码表编码")
    private String code;

    @Schema(description = "码表名称")
    private String name;

    @Schema(description = "是否树形：0否 1是")
    private String tree;

    @Schema(description = "状态：0停用 1启用")
    private String status;
}
```

- [ ] **Step 4: Create `CodeItemQueryParam.java`**

```java
package com.cat.common.entity.codeTable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "CodeItemQueryParam", description = "码项查询参数")
public class CodeItemQueryParam {
    @Schema(description = "码表ID")
    private String tableId;

    @Schema(description = "父级码项ID")
    private String parentId;

    @Schema(description = "显示文本")
    private String label;

    @Schema(description = "提交值")
    private String value;

    @Schema(description = "状态：0停用 1启用")
    private String status;
}
```

- [ ] **Step 5: Create `CodeOption.java`**

```java
package com.cat.common.entity.codeTable;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(name = "CodeOption", description = "码表选项")
public class CodeOption {
    @Schema(description = "显示文本")
    private String label;

    @Schema(description = "提交值")
    private String value;

    @Schema(description = "子选项")
    private List<CodeOption> children;
}
```

- [ ] **Step 6: Compile `common`**

Run:

```bash
mvn -pl common -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit if authorized**

Only run this commit step if commits are authorized for the session.

```bash
git add common/src/main/java/com/cat/common/entity/codeTable/CodeTable.java \
  common/src/main/java/com/cat/common/entity/codeTable/CodeItem.java \
  common/src/main/java/com/cat/common/entity/codeTable/CodeTablePageParam.java \
  common/src/main/java/com/cat/common/entity/codeTable/CodeItemQueryParam.java \
  common/src/main/java/com/cat/common/entity/codeTable/CodeOption.java
git commit -m "feat: add code table models"
```

---

## Task 2: Add mapper and service contracts

**Files:**
- Create: `simple/src/main/java/com/cat/simple/codeTable/mapper/CodeTableMapper.java`
- Create: `simple/src/main/java/com/cat/simple/codeTable/mapper/CodeItemMapper.java`
- Create: `simple/src/main/java/com/cat/simple/codeTable/service/CodeTableService.java`

- [ ] **Step 1: Create `CodeTableMapper.java`**

```java
package com.cat.simple.codeTable.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.codeTable.CodeTable;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CodeTableMapper extends BaseMapper<CodeTable> {
}
```

- [ ] **Step 2: Create `CodeItemMapper.java`**

```java
package com.cat.simple.codeTable.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.codeTable.CodeItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CodeItemMapper extends BaseMapper<CodeItem> {
}
```

- [ ] **Step 3: Create `CodeTableService.java`**

```java
package com.cat.simple.codeTable.service;

import com.cat.common.entity.Page;
import com.cat.common.entity.codeTable.CodeItem;
import com.cat.common.entity.codeTable.CodeItemQueryParam;
import com.cat.common.entity.codeTable.CodeOption;
import com.cat.common.entity.codeTable.CodeTable;
import com.cat.common.entity.codeTable.CodeTablePageParam;

import java.util.List;

public interface CodeTableService {
    boolean addTable(CodeTable codeTable);

    boolean updateTable(CodeTable codeTable);

    boolean deleteTable(CodeTable codeTable);

    CodeTable tableInfo(CodeTable codeTable);

    Page<CodeTable> queryTablePage(CodeTablePageParam pageParam);

    boolean addItem(CodeItem codeItem);

    boolean updateItem(CodeItem codeItem);

    boolean deleteItem(CodeItem codeItem);

    CodeItem itemInfo(CodeItem codeItem);

    List<CodeItem> listItems(CodeItemQueryParam queryParam);

    List<CodeOption> treeItems(CodeItemQueryParam queryParam);

    List<CodeOption> options(String code);
}
```

- [ ] **Step 4: Compile `simple`**

Run:

```bash
mvn -pl simple -DskipTests compile
```

Expected: `BUILD SUCCESS`. If `simple` cannot see the new common classes, run `mvn install -pl common -DskipTests`, then rerun this compile.

- [ ] **Step 5: Commit if authorized**

Only run this commit step if commits are authorized for the session.

```bash
git add simple/src/main/java/com/cat/simple/codeTable/mapper/CodeTableMapper.java \
  simple/src/main/java/com/cat/simple/codeTable/mapper/CodeItemMapper.java \
  simple/src/main/java/com/cat/simple/codeTable/service/CodeTableService.java
git commit -m "feat: add code table service contracts"
```

---

## Task 3: Write service tests before implementation

**Files:**
- Create: `simple/src/test/java/com/cat/simple/codeTable/service/impl/CodeTableServiceImplTest.java`

- [ ] **Step 1: Create `CodeTableServiceImplTest.java`**

```java
package com.cat.simple.codeTable.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.cat.common.entity.codeTable.CodeItem;
import com.cat.common.entity.codeTable.CodeItemQueryParam;
import com.cat.common.entity.codeTable.CodeOption;
import com.cat.common.entity.codeTable.CodeTable;
import com.cat.simple.codeTable.mapper.CodeItemMapper;
import com.cat.simple.codeTable.mapper.CodeTableMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CodeTableServiceImplTest {

    private CodeTableMapper codeTableMapper;
    private CodeItemMapper codeItemMapper;
    private CodeTableServiceImpl service;

    @BeforeEach
    void setUp() {
        codeTableMapper = mock(CodeTableMapper.class);
        codeItemMapper = mock(CodeItemMapper.class);
        service = new CodeTableServiceImpl();
        ReflectionTestUtils.setField(service, "codeTableMapper", codeTableMapper);
        ReflectionTestUtils.setField(service, "codeItemMapper", codeItemMapper);
    }

    @Test
    void addTableRejectsInvalidCode() {
        CodeTable table = baseTable().setCode("1bad");

        assertThrows(IllegalArgumentException.class, () -> service.addTable(table));
    }

    @Test
    void addTableRejectsDuplicateCode() {
        CodeTable table = baseTable();
        when(codeTableMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThrows(IllegalArgumentException.class, () -> service.addTable(table));
    }

    @Test
    void addTableSetsDefaultsAndInserts() {
        CodeTable table = baseTable().setTree(null).setStatus(null);
        when(codeTableMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(codeTableMapper.insert(table)).thenReturn(1);

        assertTrue(service.addTable(table));
        assertEquals("0", table.getTree());
        assertEquals("1", table.getStatus());
        assertEquals("0", table.getDeleted());
        verify(codeTableMapper).insert(table);
    }

    @Test
    void addItemRejectsParentForFlatTable() {
        CodeTable table = baseTable().setTree("0");
        CodeItem item = baseItem().setParentId("parentA");
        when(codeTableMapper.selectById("tableA")).thenReturn(table);

        assertThrows(IllegalArgumentException.class, () -> service.addItem(item));
    }

    @Test
    void addItemRejectsParentFromOtherTable() {
        CodeTable table = baseTable().setTree("1");
        CodeItem parent = baseItem().setId("parentA").setTableId("otherTable");
        CodeItem item = baseItem().setParentId("parentA");
        when(codeTableMapper.selectById("tableA")).thenReturn(table);
        when(codeItemMapper.selectById("parentA")).thenReturn(parent);

        assertThrows(IllegalArgumentException.class, () -> service.addItem(item));
    }

    @Test
    void addItemRejectsDuplicateValueInSameTable() {
        CodeTable table = baseTable().setTree("1");
        CodeItem item = baseItem();
        when(codeTableMapper.selectById("tableA")).thenReturn(table);
        when(codeItemMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThrows(IllegalArgumentException.class, () -> service.addItem(item));
    }

    @Test
    void updateItemRejectsSelfAsParent() {
        CodeTable table = baseTable().setTree("1");
        CodeItem oldItem = baseItem().setId("itemA");
        CodeItem item = baseItem().setId("itemA").setParentId("itemA");
        when(codeItemMapper.selectById("itemA")).thenReturn(oldItem);
        when(codeTableMapper.selectById("tableA")).thenReturn(table);

        assertThrows(IllegalArgumentException.class, () -> service.updateItem(item));
    }

    @Test
    void updateItemRejectsDescendantAsParent() {
        CodeTable table = baseTable().setTree("1");
        CodeItem oldItem = baseItem().setId("parentA");
        CodeItem item = baseItem().setId("parentA").setParentId("childA");
        CodeItem child = baseItem().setId("childA").setParentId("parentA");
        when(codeItemMapper.selectById("parentA")).thenReturn(oldItem);
        when(codeTableMapper.selectById("tableA")).thenReturn(table);
        when(codeItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(child));

        assertThrows(IllegalArgumentException.class, () -> service.updateItem(item));
    }

    @Test
    void deleteTableDeletesItemsBeforeTable() {
        CodeTable table = baseTable().setId("tableA");
        when(codeTableMapper.selectById("tableA")).thenReturn(table);
        when(codeItemMapper.delete(any(Wrapper.class))).thenReturn(2);
        when(codeTableMapper.deleteById("tableA")).thenReturn(1);

        assertTrue(service.deleteTable(new CodeTable().setId("tableA")));
        verify(codeItemMapper).delete(any(Wrapper.class));
        verify(codeTableMapper).deleteById("tableA");
    }

    @Test
    void deleteTreeItemDeletesDescendantsBeforeItem() {
        CodeItem item = baseItem().setId("parentA").setTableId("tableA");
        CodeItem child = baseItem().setId("childA").setParentId("parentA").setTableId("tableA");
        when(codeItemMapper.selectById("parentA")).thenReturn(item);
        when(codeItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(child));
        when(codeItemMapper.deleteBatchIds(ArgumentMatchers.<List<String>>any())).thenReturn(2);

        assertTrue(service.deleteItem(new CodeItem().setId("parentA")));
        verify(codeItemMapper).deleteBatchIds(List.of("parentA", "childA"));
    }

    @Test
    void optionsRejectsDisabledTable() {
        CodeTable table = baseTable().setStatus("0");
        when(codeTableMapper.selectOne(any(Wrapper.class))).thenReturn(table);

        assertThrows(IllegalArgumentException.class, () -> service.options("project_type"));
    }

    @Test
    void optionsReturnsFlatEnabledItems() {
        CodeTable table = baseTable().setTree("0").setStatus("1");
        CodeItem itemA = baseItem().setId("a").setLabel("研发项目").setValue("dev");
        CodeItem itemB = baseItem().setId("b").setLabel("实施项目").setValue("delivery");
        when(codeTableMapper.selectOne(any(Wrapper.class))).thenReturn(table);
        when(codeItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(itemA, itemB));

        List<CodeOption> options = service.options("project_type");

        assertEquals(2, options.size());
        assertEquals("研发项目", options.get(0).getLabel());
        assertEquals("dev", options.get(0).getValue());
        assertNull(options.get(0).getChildren());
    }

    @Test
    void optionsReturnsTreeAndOmitsLeafChildren() {
        CodeTable table = baseTable().setTree("1").setStatus("1");
        CodeItem country = baseItem().setId("cn").setParentId(null).setLabel("中国").setValue("cn");
        CodeItem city = baseItem().setId("bj").setParentId("cn").setLabel("北京").setValue("beijing");
        when(codeTableMapper.selectOne(any(Wrapper.class))).thenReturn(table);
        when(codeItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(country, city));

        List<CodeOption> options = service.options("area");

        assertEquals(1, options.size());
        assertEquals("中国", options.get(0).getLabel());
        assertEquals(1, options.get(0).getChildren().size());
        assertEquals("北京", options.get(0).getChildren().get(0).getLabel());
        assertNull(options.get(0).getChildren().get(0).getChildren());
    }

    @Test
    void treeItemsBuildsTreeForManagement() {
        CodeItem parent = baseItem().setId("p").setParentId(null).setLabel("父级").setValue("p");
        CodeItem child = baseItem().setId("c").setParentId("p").setLabel("子级").setValue("c");
        when(codeItemMapper.selectList(any(Wrapper.class))).thenReturn(new ArrayList<>(List.of(parent, child)));

        List<CodeOption> tree = service.treeItems(new CodeItemQueryParam().setTableId("tableA"));

        assertEquals(1, tree.size());
        assertEquals("父级", tree.get(0).getLabel());
        assertEquals("子级", tree.get(0).getChildren().get(0).getLabel());
    }

    @Test
    void addItemAcceptsValidTreeRoot() {
        CodeTable table = baseTable().setTree("1");
        CodeItem item = baseItem().setParentId(null);
        when(codeTableMapper.selectById("tableA")).thenReturn(table);
        when(codeItemMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(codeItemMapper.insert(item)).thenReturn(1);

        assertDoesNotThrow(() -> service.addItem(item));
    }

    private CodeTable baseTable() {
        return new CodeTable()
                .setId("tableA")
                .setCode("project_type")
                .setName("项目类型")
                .setTree("0")
                .setStatus("1")
                .setDeleted("0")
                .setCreateTime(LocalDateTime.now());
    }

    private CodeItem baseItem() {
        return new CodeItem()
                .setId("itemA")
                .setTableId("tableA")
                .setLabel("研发项目")
                .setValue("dev")
                .setSort(0)
                .setStatus("1")
                .setDeleted("0")
                .setCreateTime(LocalDateTime.now());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail before implementation**

Run:

```bash
mvn -pl simple -Dtest=CodeTableServiceImplTest test
```

Expected: compilation failure because `CodeTableServiceImpl` does not exist, or test failures if a partial implementation exists.

- [ ] **Step 3: Commit if authorized**

Only run this commit step if commits are authorized for the session.

```bash
git add simple/src/test/java/com/cat/simple/codeTable/service/impl/CodeTableServiceImplTest.java
git commit -m "test: cover code table service behavior"
```

---

## Task 4: Implement code-table service

**Files:**
- Create: `simple/src/main/java/com/cat/simple/codeTable/service/impl/CodeTableServiceImpl.java`
- Test: `simple/src/test/java/com/cat/simple/codeTable/service/impl/CodeTableServiceImplTest.java`

- [ ] **Step 1: Create `CodeTableServiceImpl.java`**

```java
package com.cat.simple.codeTable.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.Page;
import com.cat.common.entity.codeTable.CodeItem;
import com.cat.common.entity.codeTable.CodeItemQueryParam;
import com.cat.common.entity.codeTable.CodeOption;
import com.cat.common.entity.codeTable.CodeTable;
import com.cat.common.entity.codeTable.CodeTablePageParam;
import com.cat.simple.codeTable.mapper.CodeItemMapper;
import com.cat.simple.codeTable.mapper.CodeTableMapper;
import com.cat.simple.codeTable.service.CodeTableService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CodeTableServiceImpl implements CodeTableService {

    private static final String ENABLED = "1";
    private static final String DISABLED = "0";
    private static final String TREE = "1";
    private static final String FLAT = "0";
    private static final String NOT_DELETED = "0";
    private static final String CODE_PATTERN = "^[a-zA-Z][a-zA-Z0-9_.-]{0,63}$";

    @Resource
    private CodeTableMapper codeTableMapper;

    @Resource
    private CodeItemMapper codeItemMapper;

    @Override
    public boolean addTable(CodeTable codeTable) {
        validateTable(codeTable, false);
        if (existsTableCode(codeTable.getCode(), null)) {
            throw new IllegalArgumentException("码表编码已存在: " + codeTable.getCode());
        }
        codeTable.setId(null);
        codeTable.setDeleted(NOT_DELETED);
        LocalDateTime now = LocalDateTime.now();
        codeTable.setCreateTime(now);
        codeTable.setUpdateTime(now);
        return codeTableMapper.insert(codeTable) == 1;
    }

    @Override
    public boolean updateTable(CodeTable codeTable) {
        if (!StringUtils.hasText(codeTable.getId())) {
            throw new IllegalArgumentException("码表ID不能为空");
        }
        CodeTable old = codeTableMapper.selectById(codeTable.getId());
        if (old == null) {
            throw new IllegalArgumentException("码表不存在: " + codeTable.getId());
        }
        validateTable(codeTable, true);
        if (existsTableCode(codeTable.getCode(), codeTable.getId())) {
            throw new IllegalArgumentException("码表编码已存在: " + codeTable.getCode());
        }
        codeTable.setCreateBy(old.getCreateBy());
        codeTable.setCreateTime(old.getCreateTime());
        codeTable.setDeleted(old.getDeleted());
        codeTable.setUpdateTime(LocalDateTime.now());
        return codeTableMapper.updateById(codeTable) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTable(CodeTable codeTable) {
        if (codeTable == null || !StringUtils.hasText(codeTable.getId())) {
            throw new IllegalArgumentException("码表ID不能为空");
        }
        CodeTable old = codeTableMapper.selectById(codeTable.getId());
        if (old == null) {
            throw new IllegalArgumentException("码表不存在: " + codeTable.getId());
        }
        codeItemMapper.delete(new LambdaQueryWrapper<CodeItem>()
                .eq(CodeItem::getTableId, codeTable.getId()));
        return codeTableMapper.deleteById(codeTable.getId()) == 1;
    }

    @Override
    public CodeTable tableInfo(CodeTable codeTable) {
        if (codeTable == null || !StringUtils.hasText(codeTable.getId())) {
            throw new IllegalArgumentException("码表ID不能为空");
        }
        return codeTableMapper.selectById(codeTable.getId());
    }

    @Override
    public Page<CodeTable> queryTablePage(CodeTablePageParam pageParam) {
        Page<CodeTable> page = new Page<>(pageParam);
        LambdaQueryWrapper<CodeTable> wrapper = new LambdaQueryWrapper<CodeTable>()
                .like(StringUtils.hasText(pageParam.getCode()), CodeTable::getCode, pageParam.getCode())
                .like(StringUtils.hasText(pageParam.getName()), CodeTable::getName, pageParam.getName())
                .eq(StringUtils.hasText(pageParam.getTree()), CodeTable::getTree, pageParam.getTree())
                .eq(StringUtils.hasText(pageParam.getStatus()), CodeTable::getStatus, pageParam.getStatus())
                .orderByDesc(CodeTable::getCreateTime);
        return codeTableMapper.selectPage(page, wrapper);
    }

    @Override
    public boolean addItem(CodeItem codeItem) {
        CodeTable table = validateItem(codeItem, false);
        if (existsItemValue(codeItem.getTableId(), codeItem.getValue(), null)) {
            throw new IllegalArgumentException("码项值已存在: " + codeItem.getValue());
        }
        codeItem.setId(null);
        codeItem.setDeleted(NOT_DELETED);
        if (codeItem.getSort() == null) {
            codeItem.setSort(0);
        }
        if (FLAT.equals(table.getTree())) {
            codeItem.setParentId(null);
        }
        LocalDateTime now = LocalDateTime.now();
        codeItem.setCreateTime(now);
        codeItem.setUpdateTime(now);
        return codeItemMapper.insert(codeItem) == 1;
    }

    @Override
    public boolean updateItem(CodeItem codeItem) {
        if (codeItem == null || !StringUtils.hasText(codeItem.getId())) {
            throw new IllegalArgumentException("码项ID不能为空");
        }
        CodeItem old = codeItemMapper.selectById(codeItem.getId());
        if (old == null) {
            throw new IllegalArgumentException("码项不存在: " + codeItem.getId());
        }
        CodeTable table = validateItem(codeItem, true);
        if (existsItemValue(codeItem.getTableId(), codeItem.getValue(), codeItem.getId())) {
            throw new IllegalArgumentException("码项值已存在: " + codeItem.getValue());
        }
        if (TREE.equals(table.getTree())) {
            validateNoItemCycle(codeItem);
        }
        if (FLAT.equals(table.getTree())) {
            codeItem.setParentId(null);
        }
        codeItem.setCreateBy(old.getCreateBy());
        codeItem.setCreateTime(old.getCreateTime());
        codeItem.setDeleted(old.getDeleted());
        codeItem.setUpdateTime(LocalDateTime.now());
        return codeItemMapper.updateById(codeItem) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteItem(CodeItem codeItem) {
        if (codeItem == null || !StringUtils.hasText(codeItem.getId())) {
            throw new IllegalArgumentException("码项ID不能为空");
        }
        CodeItem old = codeItemMapper.selectById(codeItem.getId());
        if (old == null) {
            throw new IllegalArgumentException("码项不存在: " + codeItem.getId());
        }
        List<CodeItem> tableItems = codeItemMapper.selectList(new LambdaQueryWrapper<CodeItem>()
                .eq(CodeItem::getTableId, old.getTableId()));
        List<String> ids = collectSelfAndDescendantIds(old.getId(), tableItems);
        return codeItemMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public CodeItem itemInfo(CodeItem codeItem) {
        if (codeItem == null || !StringUtils.hasText(codeItem.getId())) {
            throw new IllegalArgumentException("码项ID不能为空");
        }
        return codeItemMapper.selectById(codeItem.getId());
    }

    @Override
    public List<CodeItem> listItems(CodeItemQueryParam queryParam) {
        if (queryParam == null || !StringUtils.hasText(queryParam.getTableId())) {
            throw new IllegalArgumentException("码表ID不能为空");
        }
        return codeItemMapper.selectList(new LambdaQueryWrapper<CodeItem>()
                .eq(CodeItem::getTableId, queryParam.getTableId())
                .eq(StringUtils.hasText(queryParam.getParentId()), CodeItem::getParentId, queryParam.getParentId())
                .like(StringUtils.hasText(queryParam.getLabel()), CodeItem::getLabel, queryParam.getLabel())
                .like(StringUtils.hasText(queryParam.getValue()), CodeItem::getValue, queryParam.getValue())
                .eq(StringUtils.hasText(queryParam.getStatus()), CodeItem::getStatus, queryParam.getStatus())
                .orderByAsc(CodeItem::getSort)
                .orderByAsc(CodeItem::getCreateTime));
    }

    @Override
    public List<CodeOption> treeItems(CodeItemQueryParam queryParam) {
        if (queryParam == null || !StringUtils.hasText(queryParam.getTableId())) {
            throw new IllegalArgumentException("码表ID不能为空");
        }
        List<CodeItem> items = codeItemMapper.selectList(new LambdaQueryWrapper<CodeItem>()
                .eq(CodeItem::getTableId, queryParam.getTableId())
                .eq(StringUtils.hasText(queryParam.getStatus()), CodeItem::getStatus, queryParam.getStatus())
                .orderByAsc(CodeItem::getSort)
                .orderByAsc(CodeItem::getCreateTime));
        return buildOptions(items);
    }

    @Override
    public List<CodeOption> options(String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("码表编码不能为空");
        }
        CodeTable table = codeTableMapper.selectOne(new LambdaQueryWrapper<CodeTable>()
                .eq(CodeTable::getCode, code));
        if (table == null) {
            throw new IllegalArgumentException("码表不存在: " + code);
        }
        if (!ENABLED.equals(table.getStatus())) {
            throw new IllegalArgumentException("码表已停用: " + code);
        }
        List<CodeItem> items = codeItemMapper.selectList(new LambdaQueryWrapper<CodeItem>()
                .eq(CodeItem::getTableId, table.getId())
                .eq(CodeItem::getStatus, ENABLED)
                .orderByAsc(CodeItem::getSort)
                .orderByAsc(CodeItem::getCreateTime));
        if (TREE.equals(table.getTree())) {
            return buildOptions(items);
        }
        return items.stream()
                .map(this::toOption)
                .toList();
    }

    private void validateTable(CodeTable codeTable, boolean update) {
        if (codeTable == null) {
            throw new IllegalArgumentException("码表不能为空");
        }
        if (!StringUtils.hasText(codeTable.getCode()) || !codeTable.getCode().matches(CODE_PATTERN)) {
            throw new IllegalArgumentException("码表编码格式错误");
        }
        if (!StringUtils.hasText(codeTable.getName()) || codeTable.getName().trim().length() > 64) {
            throw new IllegalArgumentException("码表名称不能为空且长度不能超过64字符");
        }
        if (!StringUtils.hasText(codeTable.getTree())) {
            codeTable.setTree(FLAT);
        }
        if (!FLAT.equals(codeTable.getTree()) && !TREE.equals(codeTable.getTree())) {
            throw new IllegalArgumentException("码表 tree 只能是 0 或 1");
        }
        if (!StringUtils.hasText(codeTable.getStatus())) {
            codeTable.setStatus(ENABLED);
        }
        if (!DISABLED.equals(codeTable.getStatus()) && !ENABLED.equals(codeTable.getStatus())) {
            throw new IllegalArgumentException("码表 status 只能是 0 或 1");
        }
        if (StringUtils.hasText(codeTable.getRemark()) && codeTable.getRemark().length() > 255) {
            throw new IllegalArgumentException("码表备注长度不能超过255字符");
        }
    }

    private CodeTable validateItem(CodeItem codeItem, boolean update) {
        if (codeItem == null) {
            throw new IllegalArgumentException("码项不能为空");
        }
        if (!StringUtils.hasText(codeItem.getTableId())) {
            throw new IllegalArgumentException("码表ID不能为空");
        }
        CodeTable table = codeTableMapper.selectById(codeItem.getTableId());
        if (table == null) {
            throw new IllegalArgumentException("码表不存在: " + codeItem.getTableId());
        }
        if (!StringUtils.hasText(codeItem.getLabel()) || codeItem.getLabel().trim().length() > 128) {
            throw new IllegalArgumentException("码项标签不能为空且长度不能超过128字符");
        }
        if (!StringUtils.hasText(codeItem.getValue()) || codeItem.getValue().length() > 128) {
            throw new IllegalArgumentException("码项值不能为空且长度不能超过128字符");
        }
        if (!StringUtils.hasText(codeItem.getStatus())) {
            codeItem.setStatus(ENABLED);
        }
        if (!DISABLED.equals(codeItem.getStatus()) && !ENABLED.equals(codeItem.getStatus())) {
            throw new IllegalArgumentException("码项 status 只能是 0 或 1");
        }
        if (StringUtils.hasText(codeItem.getRemark()) && codeItem.getRemark().length() > 255) {
            throw new IllegalArgumentException("码项备注长度不能超过255字符");
        }
        if (FLAT.equals(table.getTree()) && StringUtils.hasText(codeItem.getParentId())) {
            throw new IllegalArgumentException("平铺码表不能设置父级码项");
        }
        if (TREE.equals(table.getTree()) && StringUtils.hasText(codeItem.getParentId())) {
            CodeItem parent = codeItemMapper.selectById(codeItem.getParentId());
            if (parent == null || !codeItem.getTableId().equals(parent.getTableId())) {
                throw new IllegalArgumentException("父级码项必须属于同一码表");
            }
        }
        if (update && !StringUtils.hasText(codeItem.getId())) {
            throw new IllegalArgumentException("码项ID不能为空");
        }
        return table;
    }

    private boolean existsTableCode(String code, String excludeId) {
        LambdaQueryWrapper<CodeTable> wrapper = new LambdaQueryWrapper<CodeTable>()
                .eq(CodeTable::getCode, code)
                .ne(StringUtils.hasText(excludeId), CodeTable::getId, excludeId);
        return codeTableMapper.selectCount(wrapper) > 0;
    }

    private boolean existsItemValue(String tableId, String value, String excludeId) {
        LambdaQueryWrapper<CodeItem> wrapper = new LambdaQueryWrapper<CodeItem>()
                .eq(CodeItem::getTableId, tableId)
                .eq(CodeItem::getValue, value)
                .ne(StringUtils.hasText(excludeId), CodeItem::getId, excludeId);
        return codeItemMapper.selectCount(wrapper) > 0;
    }

    private void validateNoItemCycle(CodeItem codeItem) {
        if (!StringUtils.hasText(codeItem.getParentId())) {
            return;
        }
        if (codeItem.getId().equals(codeItem.getParentId())) {
            throw new IllegalArgumentException("父级码项不能是自己");
        }
        List<CodeItem> items = codeItemMapper.selectList(new LambdaQueryWrapper<CodeItem>()
                .eq(CodeItem::getTableId, codeItem.getTableId()));
        List<String> descendantIds = collectSelfAndDescendantIds(codeItem.getId(), items);
        descendantIds.remove(codeItem.getId());
        if (descendantIds.contains(codeItem.getParentId())) {
            throw new IllegalArgumentException("父级码项不能是自己的子孙节点");
        }
    }

    private List<String> collectSelfAndDescendantIds(String rootId, List<CodeItem> items) {
        List<String> ids = new ArrayList<>();
        ids.add(rootId);
        Map<String, List<CodeItem>> childrenMap = items.stream()
                .filter(item -> StringUtils.hasText(item.getParentId()))
                .collect(Collectors.groupingBy(CodeItem::getParentId));
        collectDescendantIds(rootId, childrenMap, ids, new HashSet<>());
        return ids;
    }

    private void collectDescendantIds(String parentId, Map<String, List<CodeItem>> childrenMap,
                                      List<String> ids, Set<String> visited) {
        if (!visited.add(parentId)) {
            return;
        }
        List<CodeItem> children = childrenMap.get(parentId);
        if (CollectionUtils.isEmpty(children)) {
            return;
        }
        for (CodeItem child : children) {
            ids.add(child.getId());
            collectDescendantIds(child.getId(), childrenMap, ids, visited);
        }
    }

    private List<CodeOption> buildOptions(List<CodeItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return List.of();
        }
        Map<String, List<CodeItem>> childrenMap = items.stream()
                .filter(item -> StringUtils.hasText(item.getParentId()))
                .collect(Collectors.groupingBy(CodeItem::getParentId));
        return items.stream()
                .filter(item -> !StringUtils.hasText(item.getParentId()))
                .map(item -> buildOption(item, childrenMap, new HashSet<>()))
                .toList();
    }

    private CodeOption buildOption(CodeItem item, Map<String, List<CodeItem>> childrenMap, Set<String> visited) {
        CodeOption option = toOption(item);
        if (!visited.add(item.getId())) {
            return option;
        }
        List<CodeItem> children = childrenMap.get(item.getId());
        if (!CollectionUtils.isEmpty(children)) {
            option.setChildren(children.stream()
                    .map(child -> buildOption(child, childrenMap, visited))
                    .toList());
        }
        return option;
    }

    private CodeOption toOption(CodeItem item) {
        return new CodeOption()
                .setLabel(item.getLabel())
                .setValue(item.getValue());
    }
}
```

- [ ] **Step 2: Run focused service tests**

Run:

```bash
mvn -pl simple -Dtest=CodeTableServiceImplTest test
```

Expected: `BUILD SUCCESS`, with all tests in `CodeTableServiceImplTest` passing.

- [ ] **Step 3: If tests fail because Mockito returns unexpected counts, update only test stubs**

Use this pattern for tests where both uniqueness checks and other count checks occur:

```java
when(codeItemMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
```

Expected after rerun: `BUILD SUCCESS`.

- [ ] **Step 4: Commit if authorized**

Only run this commit step if commits are authorized for the session.

```bash
git add simple/src/main/java/com/cat/simple/codeTable/service/impl/CodeTableServiceImpl.java
git commit -m "feat: implement code table service"
```

---

## Task 5: Add code-table controllers

**Files:**
- Create: `simple/src/main/java/com/cat/simple/codeTable/controller/CodeTableController.java`
- Create: `simple/src/main/java/com/cat/simple/codeTable/controller/CodeItemController.java`

- [ ] **Step 1: Create `CodeTableController.java`**

```java
package com.cat.simple.codeTable.controller;

import com.cat.common.entity.HttpResult;
import com.cat.common.entity.HttpResultStatus;
import com.cat.common.entity.Page;
import com.cat.common.entity.codeTable.CodeOption;
import com.cat.common.entity.codeTable.CodeTable;
import com.cat.common.entity.codeTable.CodeTablePageParam;
import com.cat.simple.codeTable.service.CodeTableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/code-table")
@Tag(name = "码表管理")
public class CodeTableController {

    @Resource
    private CodeTableService codeTableService;

    @Operation(summary = "码表分页")
    @RequestMapping(value = "/page", method = RequestMethod.POST)
    public HttpResult<Page<CodeTable>> page(@RequestBody CodeTablePageParam pageParam) {
        return HttpResult.back(codeTableService.queryTablePage(pageParam));
    }

    @Operation(summary = "新增码表")
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public HttpResult<?> add(@RequestBody CodeTable codeTable) {
        return HttpResult.back(codeTableService.addTable(codeTable) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "更新码表")
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public HttpResult<?> update(@RequestBody CodeTable codeTable) {
        return HttpResult.back(codeTableService.updateTable(codeTable) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "删除码表")
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public HttpResult<?> delete(@RequestBody CodeTable codeTable) {
        return HttpResult.back(codeTableService.deleteTable(codeTable) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "码表详情")
    @RequestMapping(value = "/detail", method = RequestMethod.POST)
    public HttpResult<CodeTable> detail(@RequestBody CodeTable codeTable) {
        return HttpResult.back(codeTableService.tableInfo(codeTable));
    }

    @Operation(summary = "码表选项")
    @Parameter(name = "code", description = "码表编码", required = true)
    @GetMapping("/options")
    public HttpResult<List<CodeOption>> options(@RequestParam("code") String code) {
        return HttpResult.back(codeTableService.options(code));
    }
}
```

- [ ] **Step 2: Create `CodeItemController.java`**

```java
package com.cat.simple.codeTable.controller;

import com.cat.common.entity.HttpResult;
import com.cat.common.entity.HttpResultStatus;
import com.cat.common.entity.codeTable.CodeItem;
import com.cat.common.entity.codeTable.CodeItemQueryParam;
import com.cat.common.entity.codeTable.CodeOption;
import com.cat.simple.codeTable.service.CodeTableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/code-item")
@Tag(name = "码项管理")
public class CodeItemController {

    @Resource
    private CodeTableService codeTableService;

    @Operation(summary = "码项列表")
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public HttpResult<List<CodeItem>> list(@RequestBody CodeItemQueryParam queryParam) {
        return HttpResult.back(codeTableService.listItems(queryParam));
    }

    @Operation(summary = "码项树")
    @RequestMapping(value = "/tree", method = RequestMethod.POST)
    public HttpResult<List<CodeOption>> tree(@RequestBody CodeItemQueryParam queryParam) {
        return HttpResult.back(codeTableService.treeItems(queryParam));
    }

    @Operation(summary = "新增码项")
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public HttpResult<?> add(@RequestBody CodeItem codeItem) {
        return HttpResult.back(codeTableService.addItem(codeItem) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "更新码项")
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public HttpResult<?> update(@RequestBody CodeItem codeItem) {
        return HttpResult.back(codeTableService.updateItem(codeItem) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "删除码项")
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public HttpResult<?> delete(@RequestBody CodeItem codeItem) {
        return HttpResult.back(codeTableService.deleteItem(codeItem) ? HttpResultStatus.SUCCESS : HttpResultStatus.ERROR);
    }

    @Operation(summary = "码项详情")
    @RequestMapping(value = "/detail", method = RequestMethod.POST)
    public HttpResult<CodeItem> detail(@RequestBody CodeItem codeItem) {
        return HttpResult.back(codeTableService.itemInfo(codeItem));
    }
}
```

- [ ] **Step 3: Compile `simple`**

Run:

```bash
mvn -pl simple -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit if authorized**

Only run this commit step if commits are authorized for the session.

```bash
git add simple/src/main/java/com/cat/simple/codeTable/controller/CodeTableController.java \
  simple/src/main/java/com/cat/simple/codeTable/controller/CodeItemController.java
git commit -m "feat: expose code table APIs"
```

---

## Task 6: Final verification and API sanity checks

**Files:**
- All files from Tasks 1-5.

- [ ] **Step 1: Run focused tests from a clean state**

Run:

```bash
mvn -pl simple clean -Dtest=CodeTableServiceImplTest test
```

Expected: `BUILD SUCCESS`, with all `CodeTableServiceImplTest` tests passing.

- [ ] **Step 2: Compile common**

Run:

```bash
mvn -pl common -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Compile simple**

Run:

```bash
mvn -pl simple -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Check changed files**

Run:

```bash
git status --short
```

Expected: the code-table files from this plan are present. Existing unrelated working-tree changes may also appear; do not stage or revert unrelated changes.

- [ ] **Step 5: Self-review implementation against spec**

Confirm:

- `cat_code_table` and `cat_code_item` are represented by entities.
- Code-table CRUD methods exist.
- Code-item CRUD methods exist.
- Flat and tree item behavior is validated.
- `GET /api/code-table/options?code=...` exists.
- Options only include enabled items.
- Disabled code tables throw business errors.
- Tree option leaves omit `children` because `CodeOption.children` uses `@JsonInclude(NON_EMPTY)`.
- Dynamic-form code was not changed for code-table support.

- [ ] **Step 6: Commit if authorized**

Only run this commit step if commits are authorized for the session.

```bash
git add common/src/main/java/com/cat/common/entity/codeTable \
  simple/src/main/java/com/cat/simple/codeTable \
  simple/src/test/java/com/cat/simple/codeTable/service/impl/CodeTableServiceImplTest.java
git commit -m "feat: add global code table module"
```

---

## Self-Review Notes

- Spec coverage: The plan covers model objects, CRUD APIs, options API, flat/tree behavior, validation rules, and tests from `docs/superpowers/specs/2026-05-24-code-table-design.md`.
- Scope: The plan does not add `optionSource.type = CODE_TABLE`, caching, tenant isolation, import/export, or dynamic-form submission existence validation.
- Type consistency: `CodeTable`, `CodeItem`, `CodeTablePageParam`, `CodeItemQueryParam`, and `CodeOption` names are consistent across entities, service, controller, and tests.
- Commit steps are included for agentic execution but must only be run when commits are explicitly authorized.
