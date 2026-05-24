package com.cat.simple.codeTable.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CodeTableServiceImpl implements CodeTableService {

    private static final String STATUS_ENABLED = "1";
    private static final String STATUS_DISABLED = "0";
    private static final String DELETED_TRUE = "1";
    private static final String DELETED_FALSE = "0";
    private static final String TREE_TRUE = "1";
    private static final String TREE_FALSE = "0";
    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_.-]{0,63}$");

    @Resource
    private CodeTableMapper codeTableMapper;

    @Resource
    private CodeItemMapper codeItemMapper;

    @Override
    public Page<CodeTable> page(CodeTablePageParam param) {
        long current = param == null || param.getCurrent() <= 0 ? 1 : param.getCurrent();
        long size = param == null || param.getSize() <= 0 ? 10 : param.getSize();
        LambdaQueryWrapper<CodeTable> wrapper = new LambdaQueryWrapper<>();
        if (param != null) {
            wrapper.like(isNotBlank(param.getCode()), CodeTable::getCode, trim(param.getCode()))
                    .like(isNotBlank(param.getName()), CodeTable::getName, trim(param.getName()))
                    .eq(isNotBlank(param.getTree()), CodeTable::getTree, trim(param.getTree()))
                    .eq(isNotBlank(param.getStatus()), CodeTable::getStatus, trim(param.getStatus()));
        }
        return codeTableMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public CodeTable addTable(CodeTable codeTable) {
        requireNonNull(codeTable, "码表不能为空");
        validateTable(codeTable);
        if (findTableByCode(codeTable.getCode()) != null) {
            throw new IllegalArgumentException("码表编码已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (isBlank(codeTable.getDeleted())) {
            codeTable.setDeleted(DELETED_FALSE);
        }
        if (codeTable.getCreateTime() == null) {
            codeTable.setCreateTime(now);
        }
        if (codeTable.getUpdateTime() == null) {
            codeTable.setUpdateTime(now);
        }
        codeTableMapper.insert(codeTable);
        return codeTable;
    }

    @Override
    public CodeTable updateTable(CodeTable codeTable) {
        requireNonNull(codeTable, "码表不能为空");
        String id = requireId(codeTable.getId(), "码表id不能为空");
        requireTable(id);
        validateTable(codeTable);
        CodeTable duplicate = findTableByCode(codeTable.getCode());
        if (duplicate != null && !Objects.equals(duplicate.getId(), id)) {
            throw new IllegalArgumentException("码表编码已存在");
        }
        codeTable.setUpdateTime(LocalDateTime.now());
        codeTableMapper.updateById(codeTable);
        return codeTable;
    }

    @Override
    @Transactional
    public void deleteTable(String id) {
        String tableId = requireId(id, "码表id不能为空");
        requireTable(tableId);
        LocalDateTime now = LocalDateTime.now();
        logicDeleteTable(tableId, now);
        codeItemMapper.update(new CodeItem(), new UpdateWrapper<CodeItem>()
                .set("deleted", DELETED_TRUE)
                .set("update_time", now)
                .eq("table_id", tableId));
    }

    @Override
    public CodeTable detailTable(String id) {
        return requireTable(requireId(id, "码表id不能为空"));
    }

    @Override
    public List<CodeOption> options(String code) {
        if (isBlank(code)) {
            throw new IllegalArgumentException("码表编码不能为空");
        }
        CodeTable table = findTableByCode(code);
        if (table == null) {
            throw new IllegalArgumentException("码表不存在: " + code);
        }
        if (STATUS_DISABLED.equals(table.getStatus())) {
            throw new IllegalArgumentException("码表已停用: " + code);
        }
        List<CodeItem> items = codeItemMapper.selectList(new LambdaQueryWrapper<CodeItem>()
                .eq(CodeItem::getTableId, table.getId())
                .eq(CodeItem::getStatus, STATUS_ENABLED)
                .orderByAsc(CodeItem::getSort)
                .orderByAsc(CodeItem::getCreateTime));
        List<CodeItem> enabledItems = safeList(items).stream()
                .filter(item -> STATUS_ENABLED.equals(item.getStatus()))
                .collect(Collectors.toList());
        if (TREE_TRUE.equals(table.getTree())) {
            return buildTreeOptions(enabledItems);
        }
        return enabledItems.stream().map(this::toOption).collect(Collectors.toList());
    }

    @Override
    public List<CodeItem> listItems(CodeItemQueryParam param) {
        requireNonNull(param, "码表项查询参数不能为空");
        String tableId = requireId(param.getTableId(), "码表id不能为空");
        return codeItemMapper.selectList(new LambdaQueryWrapper<CodeItem>()
                .eq(CodeItem::getTableId, tableId)
                .eq(param.getParentId() != null, CodeItem::getParentId, param.getParentId())
                .like(isNotBlank(param.getLabel()), CodeItem::getLabel, trim(param.getLabel()))
                .like(isNotBlank(param.getValue()), CodeItem::getValue, trim(param.getValue()))
                .eq(isNotBlank(param.getStatus()), CodeItem::getStatus, trim(param.getStatus()))
                .orderByAsc(CodeItem::getSort)
                .orderByAsc(CodeItem::getCreateTime));
    }

    @Override
    public CodeItem addItem(CodeItem codeItem) {
        requireNonNull(codeItem, "码表项不能为空");
        CodeTable table = requireTable(requireId(codeItem.getTableId(), "码表id不能为空"));
        validateItem(codeItem);
        validateParent(codeItem, table, null);
        if (findItemByTableAndValue(codeItem.getTableId(), codeItem.getValue()) != null) {
            throw new IllegalArgumentException("码表项值已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (isBlank(codeItem.getDeleted())) {
            codeItem.setDeleted(DELETED_FALSE);
        }
        if (codeItem.getCreateTime() == null) {
            codeItem.setCreateTime(now);
        }
        if (codeItem.getUpdateTime() == null) {
            codeItem.setUpdateTime(now);
        }
        codeItemMapper.insert(codeItem);
        return codeItem;
    }

    @Override
    public CodeItem updateItem(CodeItem codeItem) {
        requireNonNull(codeItem, "码表项不能为空");
        String id = requireId(codeItem.getId(), "码表项id不能为空");
        requireItem(id);
        CodeTable table = requireTable(requireId(codeItem.getTableId(), "码表id不能为空"));
        validateItem(codeItem);
        validateParent(codeItem, table, id);
        CodeItem duplicate = findItemByTableAndValue(codeItem.getTableId(), codeItem.getValue());
        if (duplicate != null && !Objects.equals(duplicate.getId(), id)) {
            throw new IllegalArgumentException("码表项值已存在");
        }
        codeItem.setUpdateTime(LocalDateTime.now());
        codeItemMapper.updateById(codeItem);
        return codeItem;
    }

    @Override
    @Transactional
    public void deleteItem(String id) {
        CodeItem item = requireItem(requireId(id, "码表项id不能为空"));
        CodeTable table = requireTable(item.getTableId());
        LocalDateTime now = LocalDateTime.now();
        Set<String> deleteIds = new HashSet<>();
        if (TREE_TRUE.equals(table.getTree())) {
            List<CodeItem> allItems = codeItemMapper.selectList(new LambdaQueryWrapper<CodeItem>()
                    .eq(CodeItem::getTableId, item.getTableId()));
            deleteIds.addAll(collectDescendantIds(item.getId(), safeList(allItems)));
        }
        deleteIds.add(item.getId());
        logicDeleteItems(deleteIds, now);
    }

    @Override
    public List<CodeOption> treeItems(CodeItemQueryParam param) {
        return buildTreeOptions(listItems(param));
    }

    private void logicDeleteTable(String tableId, LocalDateTime updateTime) {
        codeTableMapper.update(new CodeTable(), new UpdateWrapper<CodeTable>()
                .set("deleted", DELETED_TRUE)
                .set("update_time", updateTime)
                .eq("id", tableId));
    }

    private void logicDeleteItems(Set<String> itemIds, LocalDateTime updateTime) {
        if (itemIds.isEmpty()) {
            return;
        }
        codeItemMapper.update(new CodeItem(), new UpdateWrapper<CodeItem>()
                .set("deleted", DELETED_TRUE)
                .set("update_time", updateTime)
                .in("id", itemIds));
    }

    private void validateTable(CodeTable codeTable) {
        if (isBlank(codeTable.getCode())) {
            throw new IllegalArgumentException("码表编码不能为空");
        }
        String code = trim(codeTable.getCode());
        if (code.length() > 64 || !CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("码表编码格式不正确");
        }
        codeTable.setCode(code);
        if (isBlank(codeTable.getName())) {
            throw new IllegalArgumentException("码表名称不能为空");
        }
        String name = trim(codeTable.getName());
        if (name.length() > 64) {
            throw new IllegalArgumentException("码表名称长度不能超过64");
        }
        codeTable.setName(name);
        validateFlag(codeTable.getTree(), "是否树形只能为0或1");
        validateFlag(codeTable.getStatus(), "状态只能为0或1");
    }

    private void validateItem(CodeItem codeItem) {
        if (isBlank(codeItem.getLabel())) {
            throw new IllegalArgumentException("码表项标签不能为空");
        }
        String label = trim(codeItem.getLabel());
        if (label.length() > 128) {
            throw new IllegalArgumentException("码表项标签长度不能超过128");
        }
        codeItem.setLabel(label);
        if (isBlank(codeItem.getValue())) {
            throw new IllegalArgumentException("码表项值不能为空");
        }
        String value = trim(codeItem.getValue());
        if (value.length() > 128) {
            throw new IllegalArgumentException("码表项值长度不能超过128");
        }
        codeItem.setValue(value);
        validateFlag(codeItem.getStatus(), "状态只能为0或1");
        if (codeItem.getSort() == null) {
            codeItem.setSort(0);
        }
    }

    private void validateParent(CodeItem codeItem, CodeTable table, String currentItemId) {
        if (TREE_FALSE.equals(table.getTree())) {
            if (isNotBlank(codeItem.getParentId())) {
                throw new IllegalArgumentException("非树形码表项不能设置父级");
            }
            codeItem.setParentId(null);
            return;
        }
        if (isBlank(codeItem.getParentId())) {
            codeItem.setParentId(null);
            return;
        }
        String parentId = trim(codeItem.getParentId());
        if (Objects.equals(parentId, currentItemId)) {
            throw new IllegalArgumentException("父级不能是自己");
        }
        CodeItem parent = codeItemMapper.selectById(parentId);
        if (parent == null || !Objects.equals(parent.getTableId(), codeItem.getTableId())) {
            throw new IllegalArgumentException("父级码表项不存在");
        }
        if (currentItemId != null && isDescendant(currentItemId, parentId, codeItem.getTableId())) {
            throw new IllegalArgumentException("父级不能是当前项的子孙");
        }
        codeItem.setParentId(parentId);
    }

    private boolean isDescendant(String currentItemId, String parentId, String tableId) {
        List<CodeItem> allItems = codeItemMapper.selectList(new LambdaQueryWrapper<CodeItem>()
                .eq(CodeItem::getTableId, tableId));
        return collectDescendantIds(currentItemId, safeList(allItems)).contains(parentId);
    }

    private Set<String> collectDescendantIds(String id, List<CodeItem> items) {
        Map<String, List<CodeItem>> childrenByParentId = items.stream()
                .filter(item -> item.getParentId() != null)
                .collect(Collectors.groupingBy(CodeItem::getParentId, LinkedHashMap::new, Collectors.toList()));
        Set<String> ids = new HashSet<>();
        collectDescendantIds(id, childrenByParentId, ids);
        return ids;
    }

    private void collectDescendantIds(String parentId, Map<String, List<CodeItem>> childrenByParentId, Set<String> ids) {
        for (CodeItem child : childrenByParentId.getOrDefault(parentId, List.of())) {
            if (ids.add(child.getId())) {
                collectDescendantIds(child.getId(), childrenByParentId, ids);
            }
        }
    }

    private List<CodeOption> buildTreeOptions(List<CodeItem> items) {
        Map<String, List<CodeItem>> childrenByParentId = new LinkedHashMap<>();
        List<CodeItem> roots = new ArrayList<>();
        for (CodeItem item : safeList(items)) {
            if (isBlank(item.getParentId())) {
                roots.add(item);
            } else {
                childrenByParentId.computeIfAbsent(item.getParentId(), key -> new ArrayList<>()).add(item);
            }
        }
        return roots.stream()
                .map(item -> toTreeOption(item, childrenByParentId))
                .collect(Collectors.toList());
    }

    private CodeOption toTreeOption(CodeItem item, Map<String, List<CodeItem>> childrenByParentId) {
        CodeOption option = toOption(item);
        List<CodeOption> children = childrenByParentId.getOrDefault(item.getId(), List.of()).stream()
                .map(child -> toTreeOption(child, childrenByParentId))
                .collect(Collectors.toList());
        if (!children.isEmpty()) {
            option.setChildren(children);
        }
        return option;
    }

    private CodeOption toOption(CodeItem item) {
        return new CodeOption()
                .setLabel(item.getLabel())
                .setValue(item.getValue());
    }

    private CodeTable findTableByCode(String code) {
        return codeTableMapper.selectOne(new LambdaQueryWrapper<CodeTable>()
                .eq(CodeTable::getCode, trim(code)));
    }

    private CodeItem findItemByTableAndValue(String tableId, String value) {
        return codeItemMapper.selectOne(new LambdaQueryWrapper<CodeItem>()
                .eq(CodeItem::getTableId, tableId)
                .eq(CodeItem::getValue, trim(value)));
    }

    private CodeTable requireTable(String id) {
        CodeTable table = codeTableMapper.selectById(id);
        if (table == null) {
            throw new IllegalArgumentException("码表不存在");
        }
        return table;
    }

    private CodeItem requireItem(String id) {
        CodeItem item = codeItemMapper.selectById(id);
        if (item == null) {
            throw new IllegalArgumentException("码表项不存在");
        }
        return item;
    }

    private void validateFlag(String value, String message) {
        if (!STATUS_DISABLED.equals(value) && !STATUS_ENABLED.equals(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private String requireId(String id, String message) {
        if (isBlank(id)) {
            throw new IllegalArgumentException(message);
        }
        return trim(id);
    }

    private void requireNonNull(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private List<CodeItem> safeList(List<CodeItem> items) {
        return items == null ? List.of() : items;
    }
}
