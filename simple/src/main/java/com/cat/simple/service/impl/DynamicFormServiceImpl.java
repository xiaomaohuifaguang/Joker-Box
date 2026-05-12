package com.cat.simple.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.auth.LoginUser;
import com.cat.common.entity.dynamicForm.*;
import com.cat.common.utils.UUIDUtils;
import com.cat.common.utils.dynamicForm.LinkageValidator;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.mapper.*;
import com.cat.simple.service.DynamicFormService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@Service
public class DynamicFormServiceImpl implements DynamicFormService {


    @Resource
    private DynamicFormMapper dynamicFormMapper;
    @Resource
    private DynamicFormFieldMapper dynamicFormFieldMapper;
    @Resource
    private DynamicFormInstanceMapper dynamicFormInstanceMapper;
    @Resource
    private DynamicFormFieldInstanceMapper dynamicFormFieldInstanceMapper;
    @Resource
    private DynamicFormLinkageRuleMapper dynamicFormLinkageRuleMapper;
    @Resource
    private DynamicFormLinkageNodeMapper dynamicFormLinkageNodeMapper;
    @Resource
    private DynamicFormFieldGroupMapper dynamicFormFieldGroupMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean add(DynamicForm dynamicForm) {
        String userId = currentUserId();

        dynamicForm.setId(null);
        dynamicForm.setDeleted("0");
        dynamicForm.setVersion("DRAFT");
        dynamicForm.setStatus("0");
        dynamicForm.setCreateBy(userId);
        LocalDateTime now = LocalDateTime.now();
        dynamicForm.setCreateTime(now);
        dynamicForm.setUpdateTime(now);
        dynamicFormMapper.insert(dynamicForm);

        // 提取并校验所有字段（分组内 + 未分组）
        extractAndValidateFields(dynamicForm);

        // 保存分组及分组内字段
        if (!CollectionUtils.isEmpty(dynamicForm.getGroups())) {
            insertFormFieldGroupsAndFields(dynamicForm, dynamicForm.getGroups(), userId, now, "DRAFT");
        }

        // 保存未分组字段
        List<DynamicFormField> ungroupedFields = dynamicForm.getFormFields();
        if (!CollectionUtils.isEmpty(ungroupedFields)) {
            for (DynamicFormField field : ungroupedFields) {
                field.setGroupId(null);
            }
            insertFormFields(dynamicForm, ungroupedFields, userId, now, "DRAFT");
        }

        insertLinkageRules(dynamicForm, dynamicForm.getLinkageRules(), userId, now, "DRAFT");
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(DynamicForm dynamicForm) {
        DynamicForm exist = dynamicFormMapper.selectById(dynamicForm.getId());
        if (exist == null || !"0".equals(exist.getStatus())) {
            return false;
        }
        checkOwner(exist);

        // 删除所有版本的分组、字段、联动规则
        deleteGroupPhysics(exist.getId(), null);
        dynamicFormFieldMapper.delete(new LambdaQueryWrapper<DynamicFormField>()
                .eq(DynamicFormField::getFormId, exist.getId()));

        dynamicFormLinkageNodeMapper.delete(new LambdaQueryWrapper<DynamicFormLinkageNode>()
                .eq(DynamicFormLinkageNode::getFormId, exist.getId()));
        dynamicFormLinkageRuleMapper.delete(new LambdaQueryWrapper<DynamicFormLinkageRule>()
                .eq(DynamicFormLinkageRule::getFormId, exist.getId()));

        return dynamicFormMapper.deleteById(exist) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(DynamicForm dynamicForm) {
        DynamicForm oriForm = dynamicFormMapper.selectById(dynamicForm.getId());
        if (oriForm == null || (!"0".equals(oriForm.getStatus()) && !"-1".equals(oriForm.getStatus()))) {
            return false;
        }
        checkOwner(oriForm);

        LocalDateTime now = LocalDateTime.now();

        // 删除旧 DRAFT 版本的分组、字段、联动规则
        deleteGroupPhysics(oriForm.getId(), "DRAFT");
        dynamicFormFieldMapper.deletePhysicsByFormIdAndVersion(oriForm.getId(), "DRAFT");
        deleteLinkageRulesPhysics(oriForm.getId(), "DRAFT");

        oriForm.setName(dynamicForm.getName());
        oriForm.setDescription(dynamicForm.getDescription());
        oriForm.setUpdateTime(now);
        dynamicFormMapper.updateById(oriForm);

        extractAndValidateFields(dynamicForm);

        // 保存分组及分组内字段
        if (!CollectionUtils.isEmpty(dynamicForm.getGroups())) {
            insertFormFieldGroupsAndFields(dynamicForm, dynamicForm.getGroups(), oriForm.getCreateBy(), now, "DRAFT");
        }

        // 保存未分组字段
        List<DynamicFormField> ungroupedFields = dynamicForm.getFormFields();
        if (!CollectionUtils.isEmpty(ungroupedFields)) {
            for (DynamicFormField field : ungroupedFields) {
                field.setGroupId(null);
            }
            insertFormFields(dynamicForm, ungroupedFields, oriForm.getCreateBy(), now, "DRAFT");
        }

        insertLinkageRules(dynamicForm, dynamicForm.getLinkageRules(), oriForm.getCreateBy(), now, "DRAFT");
        return true;
    }

    @Override
    public DynamicForm info(DynamicForm dynamicForm) {
        DynamicForm form = dynamicFormMapper.selectById(dynamicForm.getId());
        if (form == null) {
            return null;
        }
        String version;
        if ("1".equals(form.getStatus())) {
            version = StringUtils.hasText(dynamicForm.getVersion())
                    ? dynamicForm.getVersion() : form.getVersion();
        } else {
            version = "DRAFT";
        }

        // 查分组
        List<DynamicFormFieldGroup> groups = dynamicFormFieldGroupMapper.selectList(
                new LambdaQueryWrapper<DynamicFormFieldGroup>()
                        .eq(DynamicFormFieldGroup::getFormId, form.getId())
                        .eq(DynamicFormFieldGroup::getVersion, version)
                        .eq(DynamicFormFieldGroup::getDeleted, "0")
                        .orderByAsc(DynamicFormFieldGroup::getSort));

        // 查字段
        List<DynamicFormField> fields = dynamicFormFieldMapper.selectList(
                new LambdaQueryWrapper<DynamicFormField>()
                        .eq(DynamicFormField::getFormId, form.getId())
                        .eq(DynamicFormField::getVersion, version)
                        .orderByAsc(DynamicFormField::getSort));

        // 按 groupId 分组
        Map<String, List<DynamicFormField>> groupFieldMap = fields.stream()
                .filter(f -> f.getGroupId() != null)
                .collect(Collectors.groupingBy(DynamicFormField::getGroupId, Collectors.toList()));

        // 未分组字段
        List<DynamicFormField> ungroupedFields = fields.stream()
                .filter(f -> f.getGroupId() == null)
                .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(groups)) {
            for (DynamicFormFieldGroup group : groups) {
                group.setFields(groupFieldMap.getOrDefault(group.getId(), List.of()));
            }
            form.setGroups(groups);
        } else {
            form.setGroups(null);
        }

        if (!CollectionUtils.isEmpty(ungroupedFields)) {
            form.setFormFields(ungroupedFields);
        } else {
            form.setFormFields(null);
        }

        List<DynamicFormLinkageRule> rules = loadLinkageRules(form.getId(), version);
        form.setLinkageRules(rules);

        return form;
    }

    @Override
    public Page<DynamicForm> queryPage(PageParam pageParam) {
        Page<DynamicForm> page = new Page<>(pageParam);
        return dynamicFormMapper.selectPage(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deploy(String formId) {
        DynamicForm dynamicForm = dynamicFormMapper.selectById(formId);
        if (dynamicForm == null || (!"0".equals(dynamicForm.getStatus()) && !"-1".equals(dynamicForm.getStatus()))) {
            return false;
        }
        checkOwner(dynamicForm);

        String latest = dynamicForm.getVersion();
        int next = 1;
        if (StringUtils.hasText(latest) && !"DRAFT".equals(latest)) {
            next = Integer.parseInt(latest) + 1;
        }
        String newVersion = String.valueOf(next);

        dynamicFormFieldMapper.copyVersion(formId, "DRAFT", newVersion);
        copyGroupVersion(formId, "DRAFT", newVersion);
        copyLinkageVersion(formId, "DRAFT", newVersion);

        deleteGroupPhysics(formId, "DRAFT");
        dynamicFormFieldMapper.deletePhysicsByFormIdAndVersion(formId, "DRAFT");
        deleteLinkageRulesPhysics(formId, "DRAFT");

        dynamicForm.setVersion(newVersion);
        dynamicForm.setStatus("1");
        dynamicForm.setUpdateTime(LocalDateTime.now());
        return dynamicFormMapper.updateById(dynamicForm) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean stop(String formId) {
        DynamicForm dynamicForm = dynamicFormMapper.selectById(formId);
        if (dynamicForm == null || !"1".equals(dynamicForm.getStatus())) {
            return false;
        }
        checkOwner(dynamicForm);

        String latestVersion = dynamicForm.getVersion();

        deleteGroupPhysics(formId, "DRAFT");
        dynamicFormFieldMapper.deletePhysicsByFormIdAndVersion(formId, "DRAFT");
        deleteLinkageRulesPhysics(formId, "DRAFT");

        if (StringUtils.hasText(latestVersion)) {
            dynamicFormFieldMapper.copyVersion(formId, latestVersion, "DRAFT");
            copyGroupVersion(formId, latestVersion, "DRAFT");
            copyLinkageVersion(formId, latestVersion, "DRAFT");
        }

        dynamicForm.setStatus("-1");
        dynamicForm.setUpdateTime(LocalDateTime.now());
        return dynamicFormMapper.updateById(dynamicForm) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submit(FormData formData) {
        DynamicForm dynamicForm = dynamicFormMapper.selectById(formData.getFormId());
        if (dynamicForm == null) {
            throw new IllegalArgumentException("表单不存在: " + formData.getFormId());
        }
        if (!"1".equals(dynamicForm.getStatus())) {
            throw new IllegalStateException("表单未发布, 无法提交: " + formData.getFormId());
        }

        String version = dynamicForm.getVersion();
        if (StringUtils.hasText(formData.getVersion()) && !"DRAFT".equals(formData.getVersion())) {
            version = formData.getVersion();
        }
        List<DynamicFormField> fields = dynamicFormFieldMapper.selectList(
                new LambdaQueryWrapper<DynamicFormField>()
                        .eq(DynamicFormField::getFormId, dynamicForm.getId())
                        .eq(DynamicFormField::getVersion, version));
        if (CollectionUtils.isEmpty(fields)) {
            throw new IllegalStateException("表单模板为空, 无法提交: " + formData.getFormId());
        }

        List<DynamicFormLinkageRule> rules = loadLinkageRules(dynamicForm.getId(), version);
        validateFormData(fields, formData.getData(), rules);

        String userId = currentUserId();
        LocalDateTime now = LocalDateTime.now();

        DynamicFormInstance instance = dynamicFormInstanceMapper.selectById(formData.getFormInstanceId());
        if (instance == null) {
            instance = new DynamicFormInstance()
                    .setFormId(dynamicForm.getId())
                    .setVersion(version)
                    .setCreateBy(userId)
                    .setDeleted("0")
                    .setCreateTime(now)
                    .setUpdateTime(now);
            dynamicFormInstanceMapper.insert(instance);
        } else {
            instance.setUpdateTime(now);
            dynamicFormInstanceMapper.updateById(instance);
        }

        List<DynamicFormFieldInstance> existInstances = dynamicFormFieldInstanceMapper.selectList(
                new LambdaQueryWrapper<DynamicFormFieldInstance>()
                        .eq(DynamicFormFieldInstance::getFormInstanceId, instance.getId()));
        Map<String, DynamicFormFieldInstance> existMap = existInstances.stream()
                .collect(Collectors.toMap(
                        DynamicFormFieldInstance::getFormFieldId,
                        Function.identity(),
                        (a, b) -> a));

        String finalInstanceId = instance.getId();
        List<DynamicFormFieldInstance> toSave = new ArrayList<>();

        for (DynamicFormField field : fields) {
            DynamicFormFieldInstance fieldInstance = existMap.get(field.getId());
            if (fieldInstance == null) {
                fieldInstance = new DynamicFormFieldInstance()
                        .setVersion(version)
                        .setCreateBy(userId)
                        .setCreateTime(now)
                        .setDeleted("0");
            }
            fieldInstance.setFormFieldId(field.getId())
                    .setFormInstanceId(finalInstanceId)
                    .setVal(formData.getData().get(field.getFieldId()))
                    .setUpdateTime(now);
            toSave.add(fieldInstance);
        }

        dynamicFormFieldInstanceMapper.insertOrUpdate(toSave);
        return true;
    }

    // ---------- private helpers ----------

    private String currentUserId() {
        LoginUser user = SecurityUtils.getLoginUser();
        if (user == null) {
            throw new IllegalStateException("当前未登录");
        }
        return user.getUserId();
    }

    private void checkOwner(DynamicForm form) {
        String userId = currentUserId();
        if (!userId.equals(form.getCreateBy())) {
            throw new IllegalStateException("无权操作他人表单");
        }
    }

    /**
     * 从分组结构或平铺列表中提取所有字段，并做全局唯一性/校验检查。
     */
    private List<DynamicFormField> extractAndValidateFields(DynamicForm form) {
        List<DynamicFormField> fields = new ArrayList<>();
        if (!CollectionUtils.isEmpty(form.getGroups())) {
            for (DynamicFormFieldGroup group : form.getGroups()) {
                if (!CollectionUtils.isEmpty(group.getFields())) {
                    fields.addAll(group.getFields());
                }
            }
        }
        if (!CollectionUtils.isEmpty(form.getFormFields())) {
            fields.addAll(form.getFormFields());
        }

        Set<String> fieldIds = fields.stream().map(DynamicFormField::getFieldId).collect(Collectors.toSet());
        if (fieldIds.size() != fields.size()) {
            throw new IllegalArgumentException("表单项 fieldId 存在重复");
        }
        for (DynamicFormField field : fields) {
            if (!field.validate()) {
                throw new IllegalArgumentException("表单项校验失败: " + field.getTitle());
            }
        }
        return fields;
    }

    /**
     * 保存分组及分组下的字段。
     */
    private void insertFormFieldGroupsAndFields(DynamicForm form, List<DynamicFormFieldGroup> groups,
                                                String createBy, LocalDateTime time, String version) {
        for (int i = 0; i < groups.size(); i++) {
            DynamicFormFieldGroup group = groups.get(i);
            String groupId = UUIDUtils.randomUUID();

            group.setId(groupId);
            group.setFormId(form.getId());
            group.setVersion(version);
            group.setDeleted("0");
            group.setCreateBy(createBy);
            group.setCreateTime(time);
            group.setUpdateTime(time);
            if (group.getSort() == null) {
                group.setSort(i);
            }
            dynamicFormFieldGroupMapper.insert(group);

            if (!CollectionUtils.isEmpty(group.getFields())) {
                for (DynamicFormField field : group.getFields()) {
                    field.setGroupId(groupId);
                }
                insertFormFields(form, group.getFields(), createBy, time, version);
            }
        }
    }

    private void insertFormFields(DynamicForm form, List<DynamicFormField> fields,
                                  String createBy, LocalDateTime time, String version) {
        if (CollectionUtils.isEmpty(fields)) {
            return;
        }
        for (DynamicFormField field : fields) {
            field.setId(null);
            field.setDeleted("0");
            field.setFormId(form.getId());
            field.setVersion(version);
            field.setCreateBy(createBy);
            field.setCreateTime(time);
            field.setUpdateTime(time);
        }
        dynamicFormFieldMapper.insert(fields);
    }

    private void insertLinkageRules(DynamicForm form, List<DynamicFormLinkageRule> rules,
                                    String createBy, LocalDateTime time, String version) {
        if (CollectionUtils.isEmpty(rules)) {
            return;
        }
        for (int i = 0; i < rules.size(); i++) {
            DynamicFormLinkageRule rule = rules.get(i);
            rule.setId(null);
            rule.setFormId(form.getId());
            rule.setVersion(version);
            rule.setDeleted("0");
            rule.setCreateBy(createBy);
            rule.setCreateTime(time);
            rule.setUpdateTime(time);
            if (rule.getSortOrder() == null) {
                rule.setSortOrder(i);
            }
            if (rule.getEnable() == null) {
                rule.setEnable(true);
            }

            dynamicFormLinkageRuleMapper.insert(rule);

            if (!CollectionUtils.isEmpty(rule.getConditionTree())) {
                insertNodes(form.getId(), version, rule.getId(), rule.getConditionTree(), null, createBy, time);
            }
        }
    }

    private void insertNodes(String formId, String version, String ruleId,
                             List<DynamicFormLinkageNode> nodes, String parentId,
                             String createBy, LocalDateTime time) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        for (int i = 0; i < nodes.size(); i++) {
            DynamicFormLinkageNode node = nodes.get(i);
            node.setId(null);
            node.setRuleId(ruleId);
            node.setFormId(formId);
            node.setVersion(version);
            node.setParentId(parentId);
            node.setDeleted("0");
            node.setCreateBy(createBy);
            node.setCreateTime(time);
            node.setUpdateTime(time);
            if (node.getSortOrder() == null) {
                node.setSortOrder(i);
            }
            dynamicFormLinkageNodeMapper.insert(node);

            if (!CollectionUtils.isEmpty(node.getChildren())) {
                insertNodes(formId, version, ruleId, node.getChildren(), node.getId(), createBy, time);
            }
        }
    }

    private void deleteGroupPhysics(String formId, String version) {
        dynamicFormFieldGroupMapper.deletePhysicsByFormIdAndVersion(formId, version);
    }

    private void deleteLinkageRulesPhysics(String formId, String version) {
        dynamicFormLinkageNodeMapper.deletePhysicsByFormIdAndVersion(formId, version);
        dynamicFormLinkageRuleMapper.deletePhysicsByFormIdAndVersion(formId, version);
    }

    private void copyGroupVersion(String formId, String sourceVersion, String targetVersion) {
        List<DynamicFormFieldGroup> oldGroups = dynamicFormFieldGroupMapper.selectList(
                new LambdaQueryWrapper<DynamicFormFieldGroup>()
                        .eq(DynamicFormFieldGroup::getFormId, formId)
                        .eq(DynamicFormFieldGroup::getVersion, sourceVersion)
                        .eq(DynamicFormFieldGroup::getDeleted, "0"));

        if (CollectionUtils.isEmpty(oldGroups)) {
            return;
        }

        Map<String, String> groupIdMap = new HashMap<>();
        String createBy = currentUserId();
        LocalDateTime now = LocalDateTime.now();

        for (DynamicFormFieldGroup oldGroup : oldGroups) {
            String newId = UUIDUtils.randomUUID();
            groupIdMap.put(oldGroup.getId(), newId);

            DynamicFormFieldGroup newGroup = new DynamicFormFieldGroup();
            BeanUtils.copyProperties(oldGroup, newGroup);
            newGroup.setId(newId);
            newGroup.setVersion(targetVersion);
            newGroup.setDeleted("0");
            newGroup.setCreateBy(createBy);
            newGroup.setCreateTime(now);
            newGroup.setUpdateTime(now);
            dynamicFormFieldGroupMapper.insert(newGroup);
        }

        for (Map.Entry<String, String> entry : groupIdMap.entrySet()) {
            dynamicFormFieldMapper.update(null, new LambdaUpdateWrapper<DynamicFormField>()
                    .eq(DynamicFormField::getFormId, formId)
                    .eq(DynamicFormField::getVersion, targetVersion)
                    .eq(DynamicFormField::getGroupId, entry.getKey())
                    .set(DynamicFormField::getGroupId, entry.getValue()));
        }
    }

    private void copyLinkageVersion(String formId, String sourceVersion, String targetVersion) {
        List<DynamicFormLinkageRule> oldRules = dynamicFormLinkageRuleMapper.selectList(
                new LambdaQueryWrapper<DynamicFormLinkageRule>()
                        .eq(DynamicFormLinkageRule::getFormId, formId)
                        .eq(DynamicFormLinkageRule::getVersion, sourceVersion)
                        .eq(DynamicFormLinkageRule::getDeleted, "0"));

        if (CollectionUtils.isEmpty(oldRules)) {
            return;
        }

        List<DynamicFormLinkageNode> oldNodes = dynamicFormLinkageNodeMapper.selectList(
                new LambdaQueryWrapper<DynamicFormLinkageNode>()
                        .eq(DynamicFormLinkageNode::getFormId, formId)
                        .eq(DynamicFormLinkageNode::getVersion, sourceVersion)
                        .eq(DynamicFormLinkageNode::getDeleted, "0")
                        .orderByAsc(DynamicFormLinkageNode::getSortOrder));

        Map<String, String> ruleIdMap = new HashMap<>();
        Map<String, String> nodeIdMap = new HashMap<>();
        String createBy = currentUserId();
        LocalDateTime now = LocalDateTime.now();

        for (DynamicFormLinkageRule oldRule : oldRules) {
            String newRuleId = UUIDUtils.randomUUID();
            ruleIdMap.put(oldRule.getId(), newRuleId);

            DynamicFormLinkageRule newRule = new DynamicFormLinkageRule();
            BeanUtils.copyProperties(oldRule, newRule);
            newRule.setId(newRuleId);
            newRule.setVersion(targetVersion);
            newRule.setDeleted("0");
            newRule.setCreateBy(createBy);
            newRule.setCreateTime(now);
            newRule.setUpdateTime(now);
            dynamicFormLinkageRuleMapper.insert(newRule);
        }

        for (DynamicFormLinkageNode oldNode : oldNodes) {
            nodeIdMap.put(oldNode.getId(), UUIDUtils.randomUUID());
        }

        for (DynamicFormLinkageNode oldNode : oldNodes) {
            DynamicFormLinkageNode newNode = new DynamicFormLinkageNode();
            BeanUtils.copyProperties(oldNode, newNode);
            newNode.setId(nodeIdMap.get(oldNode.getId()));
            newNode.setRuleId(ruleIdMap.get(oldNode.getRuleId()));
            newNode.setParentId(oldNode.getParentId() == null ? null : nodeIdMap.get(oldNode.getParentId()));
            newNode.setVersion(targetVersion);
            newNode.setDeleted("0");
            newNode.setCreateBy(createBy);
            newNode.setCreateTime(now);
            newNode.setUpdateTime(now);
            dynamicFormLinkageNodeMapper.insert(newNode);
        }
    }

    private List<DynamicFormLinkageRule> loadLinkageRules(String formId, String version) {
        List<DynamicFormLinkageRule> rules = dynamicFormLinkageRuleMapper.selectList(
                new LambdaQueryWrapper<DynamicFormLinkageRule>()
                        .eq(DynamicFormLinkageRule::getFormId, formId)
                        .eq(DynamicFormLinkageRule::getVersion, version)
                        .eq(DynamicFormLinkageRule::getDeleted, "0")
                        .orderByAsc(DynamicFormLinkageRule::getSortOrder));

        if (CollectionUtils.isEmpty(rules)) {
            return rules;
        }

        List<DynamicFormLinkageNode> allNodes = dynamicFormLinkageNodeMapper.selectList(
                new LambdaQueryWrapper<DynamicFormLinkageNode>()
                        .eq(DynamicFormLinkageNode::getFormId, formId)
                        .eq(DynamicFormLinkageNode::getVersion, version)
                        .eq(DynamicFormLinkageNode::getDeleted, "0")
                        .orderByAsc(DynamicFormLinkageNode::getSortOrder));

        Map<String, List<DynamicFormLinkageNode>> childrenMap = allNodes.stream()
                .filter(n -> n.getParentId() != null)
                .collect(Collectors.groupingBy(DynamicFormLinkageNode::getParentId));

        Map<String, List<DynamicFormLinkageNode>> ruleNodeMap = allNodes.stream()
                .collect(Collectors.groupingBy(DynamicFormLinkageNode::getRuleId));

        for (DynamicFormLinkageRule rule : rules) {
            List<DynamicFormLinkageNode> ruleNodes = ruleNodeMap.getOrDefault(rule.getId(), List.of());
            List<DynamicFormLinkageNode> roots = ruleNodes.stream()
                    .filter(n -> n.getParentId() == null)
                    .sorted(Comparator.comparing(DynamicFormLinkageNode::getSortOrder))
                    .toList();

            for (DynamicFormLinkageNode root : roots) {
                assembleTree(root, childrenMap);
            }
            rule.setConditionTree(roots);
        }

        return rules;
    }

    private void assembleTree(DynamicFormLinkageNode node, Map<String, List<DynamicFormLinkageNode>> childrenMap) {
        List<DynamicFormLinkageNode> children = childrenMap.get(node.getId());
        if (!CollectionUtils.isEmpty(children)) {
            children = children.stream()
                    .sorted(Comparator.comparing(DynamicFormLinkageNode::getSortOrder))
                    .toList();
            node.setChildren(children);
            for (DynamicFormLinkageNode child : children) {
                assembleTree(child, childrenMap);
            }
        }
    }

    private void validateFormData(List<DynamicFormField> fields, Map<String, Object> data,
                                  List<DynamicFormLinkageRule> rules) {
        Map<String, LinkageValidator.FieldEffect> effects = LinkageValidator.evalFieldEffects(rules, data);

        for (DynamicFormField field : fields) {
            Object value = data == null ? null : data.get(field.getFieldId());
            String strVal = value == null ? null : value.toString();

            LinkageValidator.FieldEffect effect = effects.get(field.getFieldId());
            boolean hidden = effect != null && !effect.visible();
            boolean disabled = effect != null && effect.disabled();

            if (!hidden && !disabled && "1".equals(field.getRequired())
                    && !StringUtils.hasText(strVal)) {
                throw new IllegalArgumentException(field.getTitle() + " 必填");
            }

            if (!StringUtils.hasText(strVal) || hidden || disabled) {
                continue;
            }

            if (field.getMinLength() != null && strVal.length() < field.getMinLength()) {
                throw new IllegalArgumentException(
                        field.getTitle() + " 长度不能小于 " + field.getMinLength());
            }
            if (field.getMaxLength() != null && strVal.length() > field.getMaxLength()) {
                throw new IllegalArgumentException(
                        field.getTitle() + " 长度不能大于 " + field.getMaxLength());
            }

            if (field.getType() == DynamicFormFieldType.NUMBER) {
                try {
                    double num = Double.parseDouble(strVal);
                    if (field.getMin() != null && num < field.getMin()) {
                        throw new IllegalArgumentException(
                                field.getTitle() + " 不能小于 " + field.getMin());
                    }
                    if (field.getMax() != null && num > field.getMax()) {
                        throw new IllegalArgumentException(
                                field.getTitle() + " 不能大于 " + field.getMax());
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(field.getTitle() + " 必须为数字");
                }
            }

            String effectivePattern = effect != null && effect.pattern() != null
                    ? effect.pattern() : field.getPattern();
            String effectivePatternTips = effect != null && effect.patternTips() != null
                    ? effect.patternTips() : field.getPatternTips();

            if (StringUtils.hasText(effectivePattern)) {
                try {
                    if (!Pattern.matches(effectivePattern, strVal)) {
                        throw new IllegalArgumentException(
                                StringUtils.hasText(effectivePatternTips)
                                        ? effectivePatternTips
                                        : field.getTitle() + " 格式不正确");
                    }
                } catch (PatternSyntaxException e) {
                    throw new IllegalArgumentException(
                            field.getTitle() + " 正则表达式配置错误");
                }
            }
        }
    }
}
