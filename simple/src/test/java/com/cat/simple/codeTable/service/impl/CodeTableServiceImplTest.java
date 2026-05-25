package com.cat.simple.codeTable.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.cat.common.entity.codeTable.CodeItem;
import com.cat.common.entity.codeTable.CodeOption;
import com.cat.common.entity.codeTable.CodeTable;
import com.cat.simple.codeTable.mapper.CodeItemMapper;
import com.cat.simple.codeTable.mapper.CodeTableMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodeTableServiceImplTest {

    @Mock
    private CodeTableMapper codeTableMapper;

    @Mock
    private CodeItemMapper codeItemMapper;

    private CodeTableServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CodeTableServiceImpl();
        ReflectionTestUtils.setField(service, "codeTableMapper", codeTableMapper);
        ReflectionTestUtils.setField(service, "codeItemMapper", codeItemMapper);
    }

    @Test
    void addTableRejectsBlankOrInvalidCodeNameTreeAndStatus() {
        assertThrows(IllegalArgumentException.class, () -> service.addTable(table("table-1", "", "状态", "0", "1")));
        assertThrows(IllegalArgumentException.class, () -> service.addTable(table("table-1", "1status", "状态", "0", "1")));
        assertThrows(IllegalArgumentException.class, () -> service.addTable(table("table-1", "status", " ", "0", "1")));
        assertThrows(IllegalArgumentException.class, () -> service.addTable(table("table-1", "status", "状态", "2", "1")));
        assertThrows(IllegalArgumentException.class, () -> service.addTable(table("table-1", "status", "状态", "0", "2")));

        verify(codeTableMapper, never()).insert(any(CodeTable.class));
    }

    @Test
    void addTableRejectsDuplicateCodeAmongNonDeletedRecords() {
        lenient().when(codeTableMapper.selectOne(any())).thenReturn(table("existing", "status", "状态", "0", "1"));
        lenient().when(codeTableMapper.selectCount(any())).thenReturn(1L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.addTable(table(null, "status", "状态", "0", "1")));

        assertTrue(exception.getMessage().contains("已存在"));
        verify(codeTableMapper, never()).insert(any(CodeTable.class));
    }

    @Test
    void addTableInsertsAndReturnsValidatedTable() {
        CodeTable input = table(null, "status", "状态", "0", "1");
        when(codeTableMapper.selectOne(any())).thenReturn(null);
        when(codeTableMapper.insert(any(CodeTable.class))).thenReturn(1);

        CodeTable result = service.addTable(input);

        ArgumentCaptor<CodeTable> tableCaptor = ArgumentCaptor.forClass(CodeTable.class);
        verify(codeTableMapper).insert(tableCaptor.capture());
        assertEquals("status", tableCaptor.getValue().getCode());
        assertEquals("状态", tableCaptor.getValue().getName());
        assertEquals("0", tableCaptor.getValue().getTree());
        assertEquals("1", tableCaptor.getValue().getStatus());
        assertEquals("0", tableCaptor.getValue().getDeleted());
        assertEquals(tableCaptor.getValue(), result);
    }

    @Test
    void addItemRejectsMissingTableIdOrTableNotFound() {
        assertThrows(IllegalArgumentException.class, () -> service.addItem(item("item-1", "", null, "启用", "enabled", 1, "1")));

        when(codeTableMapper.selectById("table-1")).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> service.addItem(item("item-1", "table-1", null, "启用", "enabled", 1, "1")));

        verify(codeItemMapper, never()).insert(any(CodeItem.class));
    }

    @Test
    void addItemRejectsParentIdForFlatTable() {
        when(codeTableMapper.selectById("table-1")).thenReturn(table("table-1", "status", "状态", "0", "1"));

        assertThrows(IllegalArgumentException.class,
                () -> service.addItem(item("item-1", "table-1", "parent-1", "启用", "enabled", 1, "1")));

        verify(codeItemMapper, never()).insert(any(CodeItem.class));
    }

    @Test
    void addItemRejectsParentIdFromAnotherTableForTreeTable() {
        when(codeTableMapper.selectById("table-1")).thenReturn(table("table-1", "area", "区域", "1", "1"));
        when(codeItemMapper.selectById("parent-1"))
                .thenReturn(item("parent-1", "another-table", null, "父级", "parent", 1, "1"));

        assertThrows(IllegalArgumentException.class,
                () -> service.addItem(item("item-1", "table-1", "parent-1", "子级", "child", 2, "1")));

        verify(codeItemMapper, never()).insert(any(CodeItem.class));
    }

    @Test
    void addItemRejectsDuplicateValueWithinSameTable() {
        when(codeTableMapper.selectById("table-1")).thenReturn(table("table-1", "status", "状态", "0", "1"));
        lenient().when(codeItemMapper.selectOne(any())).thenReturn(item("existing", "table-1", null, "启用", "enabled", 1, "1"));
        lenient().when(codeItemMapper.selectCount(any())).thenReturn(1L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.addItem(item("item-1", "table-1", null, "启用", "enabled", 1, "1")));

        assertTrue(exception.getMessage().contains("已存在"));
        verify(codeItemMapper, never()).insert(any(CodeItem.class));
    }

    @Test
    void addItemInsertsAndReturnsValidatedItem() {
        CodeItem input = item(null, "table-1", null, "启用", "enabled", 1, "1");
        when(codeTableMapper.selectById("table-1")).thenReturn(table("table-1", "status", "状态", "0", "1"));
        when(codeItemMapper.selectOne(any())).thenReturn(null);
        when(codeItemMapper.insert(any(CodeItem.class))).thenReturn(1);

        CodeItem result = service.addItem(input);

        ArgumentCaptor<CodeItem> itemCaptor = ArgumentCaptor.forClass(CodeItem.class);
        verify(codeItemMapper).insert(itemCaptor.capture());
        assertEquals("table-1", itemCaptor.getValue().getTableId());
        assertEquals("启用", itemCaptor.getValue().getLabel());
        assertEquals("enabled", itemCaptor.getValue().getValue());
        assertEquals(1, itemCaptor.getValue().getSort());
        assertEquals("1", itemCaptor.getValue().getStatus());
        assertEquals("0", itemCaptor.getValue().getDeleted());
        assertEquals(itemCaptor.getValue(), result);
    }

    @Test
    void updateItemRejectsSettingParentIdToSelf() {
        when(codeItemMapper.selectById("item-1"))
                .thenReturn(item("item-1", "table-1", null, "当前", "current", 1, "1"));
        lenient().when(codeTableMapper.selectById("table-1")).thenReturn(table("table-1", "area", "区域", "1", "1"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.updateItem(item("item-1", "table-1", "item-1", "当前", "current", 1, "1")));

        assertTrue(exception.getMessage().contains("自己"));
        verify(codeItemMapper, never()).updateById(any(CodeItem.class));
    }

    @Test
    void updateItemRejectsSettingParentIdToDescendantNode() {
        CodeItem current = item("item-1", "table-1", null, "当前", "current", 1, "1");
        CodeItem child = item("child-1", "table-1", "item-1", "子级", "child", 2, "1");
        CodeItem grandchild = item("grandchild-1", "table-1", "child-1", "孙级", "grandchild", 3, "1");

        when(codeItemMapper.selectById("item-1")).thenReturn(current);
        lenient().when(codeTableMapper.selectById("table-1")).thenReturn(table("table-1", "area", "区域", "1", "1"));
        when(codeItemMapper.selectById("grandchild-1")).thenReturn(grandchild);
        when(codeItemMapper.selectList(any())).thenReturn(List.of(current, child, grandchild));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.updateItem(item("item-1", "table-1", "grandchild-1", "当前", "current", 1, "1")));

        assertTrue(exception.getMessage().contains("子孙"));
        verify(codeItemMapper, never()).updateById(any(CodeItem.class));
    }

    @Test
    void deleteTableLogicallyDeletesTableAndAllItemsUnderIt() {
        when(codeTableMapper.selectById("table-1")).thenReturn(table("table-1", "status", "状态", "0", "1"));
        service.deleteTable("table-1");

        verify(codeTableMapper).update(any(CodeTable.class), any(UpdateWrapper.class));
        verify(codeItemMapper).update(any(CodeItem.class), any(UpdateWrapper.class));
        verify(codeTableMapper, never()).updateById(any(CodeTable.class));
        verify(codeItemMapper, never()).updateById(any(CodeItem.class));
    }

    @Test
    void deleteItemOnTreeDeletesCurrentItemAndDescendants() {
        CodeItem current = item("item-1", "table-1", null, "当前", "current", 1, "1");
        CodeItem child = item("child-1", "table-1", "item-1", "子级", "child", 2, "1");
        CodeItem grandchild = item("grandchild-1", "table-1", "child-1", "孙级", "grandchild", 3, "1");
        CodeItem sibling = item("sibling-1", "table-1", null, "同级", "sibling", 4, "1");

        when(codeItemMapper.selectById("item-1")).thenReturn(current);
        when(codeTableMapper.selectById("table-1")).thenReturn(table("table-1", "area", "区域", "1", "1"));
        when(codeItemMapper.selectList(any())).thenReturn(List.of(current, child, grandchild, sibling));

        service.deleteItem("item-1");

        verify(codeItemMapper).update(any(CodeItem.class), any(UpdateWrapper.class));
        verify(codeItemMapper, never()).updateById(any(CodeItem.class));
    }

    @Test
    void optionsReturnsFlatOneDimensionalOptionsSortedByMapperResultOrder() {
        when(codeTableMapper.selectOne(any())).thenReturn(table("table-1", "status", "状态", "0", "1"));
        when(codeItemMapper.selectList(any())).thenReturn(List.of(
                item("item-2", "table-1", null, "停用", "disabled", 2, "1"),
                item("item-1", "table-1", null, "启用", "enabled", 1, "1")
        ));

        List<CodeOption> options = service.options("status");

        assertEquals(2, options.size());
        assertEquals("停用", options.get(0).getLabel());
        assertEquals("disabled", options.get(0).getValue());
        assertNullOrEmpty(options.get(0).getChildren());
        assertEquals("启用", options.get(1).getLabel());
        assertEquals("enabled", options.get(1).getValue());
        assertNullOrEmpty(options.get(1).getChildren());
    }

    @Test
    void optionsReturnsNestedTreeOptionsAndOmitsChildrenOnLeaves() {
        when(codeTableMapper.selectOne(any())).thenReturn(table("table-1", "area", "区域", "1", "1"));
        when(codeItemMapper.selectList(any())).thenReturn(List.of(
                item("root-1", "table-1", null, "中国", "cn", 1, "1"),
                item("child-1", "table-1", "root-1", "浙江", "zj", 2, "1"),
                item("leaf-1", "table-1", "child-1", "杭州", "hz", 3, "1"),
                item("root-2", "table-1", null, "美国", "us", 4, "1")
        ));

        List<CodeOption> options = service.options("area");

        assertEquals(2, options.size());
        assertEquals("中国", options.get(0).getLabel());
        assertEquals("cn", options.get(0).getValue());
        assertEquals(1, options.get(0).getChildren().size());
        CodeOption province = options.get(0).getChildren().get(0);
        assertEquals("浙江", province.getLabel());
        assertEquals("zj", province.getValue());
        assertEquals(1, province.getChildren().size());
        CodeOption city = province.getChildren().get(0);
        assertEquals("杭州", city.getLabel());
        assertEquals("hz", city.getValue());
        assertNullOrEmpty(city.getChildren());
        assertEquals("美国", options.get(1).getLabel());
        assertNullOrEmpty(options.get(1).getChildren());
    }

    @Test
    void optionsRejectsDisabledTableWithExpectedMessage() {
        when(codeTableMapper.selectOne(any())).thenReturn(table("table-1", "status", "状态", "0", "0"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.options("status"));

        assertTrue(exception.getMessage().contains("码表已停用"));
    }

    @Test
    void optionsReturnsEnabledMapperRows() {
        when(codeTableMapper.selectOne(any())).thenReturn(table("table-1", "status", "状态", "0", "1"));
        when(codeItemMapper.selectList(any())).thenReturn(List.of(
                item("item-1", "table-1", null, "启用", "enabled", 1, "1")
        ));

        List<CodeOption> options = service.options("status");

        assertEquals(1, options.size());
        assertEquals("启用", options.get(0).getLabel());
        assertEquals("enabled", options.get(0).getValue());
    }

    private static CodeTable table(String id, String code, String name, String tree, String status) {
        return new CodeTable()
                .setId(id)
                .setCode(code)
                .setName(name)
                .setTree(tree)
                .setStatus(status)
                .setDeleted("0");
    }

    private static CodeItem item(String id, String tableId, String parentId, String label, String value, Integer sort, String status) {
        return new CodeItem()
                .setId(id)
                .setTableId(tableId)
                .setParentId(parentId)
                .setLabel(label)
                .setValue(value)
                .setSort(sort)
                .setStatus(status)
                .setDeleted("0");
    }

    private static void assertNullOrEmpty(List<CodeOption> children) {
        if (children != null) {
            assertTrue(children.isEmpty());
        }
    }
}
