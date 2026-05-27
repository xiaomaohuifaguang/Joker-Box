package com.cat.simple.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.dynamicForm.DynamicFormField;
import com.cat.common.entity.dynamicForm.DynamicFormFieldGroup;
import com.cat.common.entity.dynamicForm.DynamicFormFieldInstance;
import com.cat.common.entity.dynamicForm.DynamicFormInstance;
import com.cat.common.entity.process.*;
import com.cat.simple.config.flowable.guard.ProcessGuard;
import com.cat.simple.form.mapper.DynamicFormFieldGroupMapper;
import com.cat.simple.form.mapper.DynamicFormFieldInstanceMapper;
import com.cat.simple.form.mapper.DynamicFormFieldMapper;
import com.cat.simple.form.mapper.DynamicFormInstanceMapper;
import com.cat.simple.process.mapper.ProcessDefinitionFormMapper;
import com.cat.simple.process.mapper.ProcessDefinitionMapper;
import com.cat.simple.process.mapper.ProcessInstanceFormMapper;
import com.cat.simple.process.mapper.ProcessNodeFieldPermissionMapper;
import com.cat.simple.process.service.ProcessFormService;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程表单服务实现 — 封装表单实例与流程绑定、表单数据读写、任务表单渲染数据组装。
 */
@Service
public class ProcessFormServiceImpl implements ProcessFormService {

    @Resource
    private ProcessInstanceFormMapper processInstanceFormMapper;
    @Resource
    private ProcessDefinitionFormMapper processDefinitionFormMapper;
    @Resource
    private ProcessNodeFieldPermissionMapper processNodeFieldPermissionMapper;
    @Resource
    private DynamicFormInstanceMapper dynamicFormInstanceMapper;
    @Resource
    private DynamicFormFieldInstanceMapper dynamicFormFieldInstanceMapper;
    @Resource
    private DynamicFormFieldMapper dynamicFormFieldMapper;
    @Resource
    private DynamicFormFieldGroupMapper dynamicFormFieldGroupMapper;
    @Resource
    private ProcessDefinitionMapper processDefinitionMapper;
    @Resource
    private ProcessGuard guard;

    // ========== 表单配置内部类 ==========

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class FormConfig {
        /** 节点级别表单绑定（可能为 null） */
        private ProcessDefinitionForm nodeBinding;
        /** 全局级别表单绑定（可能为 null） */
        private ProcessDefinitionForm globalBinding;
        /** 当前节点是否继承主表单 */
        private boolean inheritMainForm;
        /** 当前节点的字段权限列表 */
        private List<ProcessNodeFieldPermission> fieldPermissions;
    }

    // ========== 公开接口实现 ==========

    @Override
    @Transactional
    public ProcessInstanceForm createFormInstanceIfNeeded(Integer processInstanceId,
                                                          Integer processDefinitionId,
                                                          String nodeId) {
        FormConfig config = resolveFormConfig(processDefinitionId, nodeId);
        if (config == null) {
            return null;
        }

        // 确定绑定来源：节点绑定优先，否则取全局绑定
        ProcessDefinitionForm effectiveBinding = config.getNodeBinding() != null
                ? config.getNodeBinding()
                : config.getGlobalBinding();
        if (effectiveBinding == null) {
            return null;
        }

        // 检查关联表是否已存在该节点的记录（驳回场景复用）
        ProcessInstanceForm existingRelation = processInstanceFormMapper.selectOne(
                new LambdaQueryWrapper<ProcessInstanceForm>()
                        .eq(ProcessInstanceForm::getProcessInstanceId, processInstanceId)
                        .eq(ProcessInstanceForm::getNodeId, nodeId));
        if (existingRelation != null) {
            return existingRelation;
        }

        String currentUserId = guard.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        // 创建节点表单实例
        String formInstanceId = createDynamicFormInstance(
                effectiveBinding.getFormId(), effectiveBinding.getFormVersion(), currentUserId, now);

        // 初始化所有字段实例（空值）
        initFieldInstances(formInstanceId, effectiveBinding.getFormId(),
                effectiveBinding.getFormVersion(), currentUserId, now);

        // 插入关联记录
        ProcessInstanceForm relation = new ProcessInstanceForm()
                .setProcessInstanceId(processInstanceId)
                .setNodeId(nodeId)
                .setFormId(effectiveBinding.getFormId())
                .setFormVersion(effectiveBinding.getFormVersion())
                .setFormInstanceId(formInstanceId)
                .setCreateBy(currentUserId)
                .setCreateTime(now);
        processInstanceFormMapper.insert(relation);

        // 如果继承主表单，且全局绑定存在且与节点绑定不同，也创建全局表单实例
        if (config.isInheritMainForm() && config.getGlobalBinding() != null
                && !config.getGlobalBinding().getFormId().equals(effectiveBinding.getFormId())) {

            // 检查全局表单实例是否已存在
            ProcessInstanceForm existingGlobal = processInstanceFormMapper.selectOne(
                    new LambdaQueryWrapper<ProcessInstanceForm>()
                            .eq(ProcessInstanceForm::getProcessInstanceId, processInstanceId)
                            .isNull(ProcessInstanceForm::getNodeId));
            if (existingGlobal == null) {
                String globalFormInstanceId = createDynamicFormInstance(
                        config.getGlobalBinding().getFormId(),
                        config.getGlobalBinding().getFormVersion(),
                        currentUserId, now);

                initFieldInstances(globalFormInstanceId, config.getGlobalBinding().getFormId(),
                        config.getGlobalBinding().getFormVersion(), currentUserId, now);

                ProcessInstanceForm globalRelation = new ProcessInstanceForm()
                        .setProcessInstanceId(processInstanceId)
                        .setNodeId(null)
                        .setFormId(config.getGlobalBinding().getFormId())
                        .setFormVersion(config.getGlobalBinding().getFormVersion())
                        .setFormInstanceId(globalFormInstanceId)
                        .setCreateBy(currentUserId)
                        .setCreateTime(now);
                processInstanceFormMapper.insert(globalRelation);
            }
        }

        return relation;
    }

    @Override
    @Transactional
    public void writeFormData(Integer processInstanceId, Integer processDefinitionId,
                              String nodeId, Map<String, Object> formData, boolean skipRequired) {
        if (CollectionUtils.isEmpty(formData)) {
            return;
        }

        FormConfig config = resolveFormConfig(processDefinitionId, nodeId);
        if (config == null) {
            return;
        }

        // 确定绑定来源
        ProcessDefinitionForm effectiveBinding = config.getNodeBinding() != null
                ? config.getNodeBinding()
                : config.getGlobalBinding();
        if (effectiveBinding == null) {
            return;
        }

        // 检查关联记录是否存在
        ProcessInstanceForm relation = processInstanceFormMapper.selectOne(
                new LambdaQueryWrapper<ProcessInstanceForm>()
                        .eq(ProcessInstanceForm::getProcessInstanceId, processInstanceId)
                        .eq(ProcessInstanceForm::getNodeId, nodeId));
        if (relation == null) {
            return;
        }

        // 构建字段权限 map: fieldKey -> permission
        Map<String, String> permissionMap = config.getFieldPermissions().stream()
                .collect(Collectors.toMap(
                        ProcessNodeFieldPermission::getFieldKey,
                        ProcessNodeFieldPermission::getPermission,
                        (a, b) -> b));

        // 加载字段定义
        List<DynamicFormField> fieldDefs = dynamicFormFieldMapper.selectList(
                new LambdaQueryWrapper<DynamicFormField>()
                        .eq(DynamicFormField::getFormId, effectiveBinding.getFormId())
                        .eq(DynamicFormField::getVersion, effectiveBinding.getFormVersion()));

        // 加载已有字段实例
        List<DynamicFormFieldInstance> existingInstances = dynamicFormFieldInstanceMapper.selectList(
                new LambdaQueryWrapper<DynamicFormFieldInstance>()
                        .eq(DynamicFormFieldInstance::getFormInstanceId, relation.getFormInstanceId()));

        // 构建 fieldId -> DB主键id 的映射
        Map<String, String> fieldIdToDbId = fieldDefs.stream()
                .collect(Collectors.toMap(DynamicFormField::getFieldId, DynamicFormField::getId, (a, b) -> b));

        // 构建 DB主键id -> 字段实例 的映射
        Map<String, DynamicFormFieldInstance> dbIdToInstance = existingInstances.stream()
                .collect(Collectors.toMap(DynamicFormFieldInstance::getFormFieldId, f -> f, (a, b) -> b));

        // 构建 fieldKey -> DynamicFormField 映射
        Map<String, DynamicFormField> fieldKeyToDef = fieldDefs.stream()
                .collect(Collectors.toMap(DynamicFormField::getFieldId, f -> f, (a, b) -> b));

        String currentUserId = guard.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        // 遍历 formData，按权限过滤，校验必填，upsert
        List<DynamicFormFieldInstance> toInsert = new ArrayList<>();
        List<DynamicFormFieldInstance> toUpdate = new ArrayList<>();

        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            String fieldKey = entry.getKey();
            Object value = entry.getValue();

            DynamicFormField fieldDef = fieldKeyToDef.get(fieldKey);
            if (fieldDef == null) {
                continue; // 忽略不存在的字段
            }

            // 检查权限：READONLY 和 HIDDEN 跳过写入
            String permission = permissionMap.getOrDefault(fieldKey, "VISIBLE");
            if ("READONLY".equals(permission) || "HIDDEN".equals(permission)) {
                continue;
            }

            // 校验必填
            if (!skipRequired) {
                boolean required = "REQUIRED".equals(permission) || "1".equals(fieldDef.getRequired());
                if (required && !hasValue(value)) {
                    throw new IllegalArgumentException("字段 [" + fieldDef.getTitle() + "] 为必填项");
                }
            }

            // 查找已有实例
            String dbId = fieldIdToDbId.get(fieldKey);
            if (dbId == null) {
                continue;
            }

            DynamicFormFieldInstance instance = dbIdToInstance.get(dbId);
            if (instance != null) {
                // 更新
                instance.setVal(value);
                instance.setUpdateTime(now);
                toUpdate.add(instance);
            } else {
                // 新增
                DynamicFormFieldInstance newInstance = new DynamicFormFieldInstance()
                        .setFormFieldId(dbId)
                        .setFormInstanceId(relation.getFormInstanceId())
                        .setVal(value)
                        .setVersion(effectiveBinding.getFormVersion())
                        .setCreateBy(currentUserId)
                        .setCreateTime(now);
                toInsert.add(newInstance);
            }
        }

        // 批量插入和更新
        if (!toInsert.isEmpty()) {
            for (DynamicFormFieldInstance inst : toInsert) {
                dynamicFormFieldInstanceMapper.insert(inst);
            }
        }
        if (!toUpdate.isEmpty()) {
            for (DynamicFormFieldInstance inst : toUpdate) {
                dynamicFormFieldInstanceMapper.updateById(inst);
            }
        }

        // 如果继承主表单，也写入全局表单适用的字段数据
        if (config.isInheritMainForm() && config.getGlobalBinding() != null
                && !config.getGlobalBinding().getFormId().equals(effectiveBinding.getFormId())) {

            ProcessInstanceForm globalRelation = processInstanceFormMapper.selectOne(
                    new LambdaQueryWrapper<ProcessInstanceForm>()
                            .eq(ProcessInstanceForm::getProcessInstanceId, processInstanceId)
                            .isNull(ProcessInstanceForm::getNodeId));
            if (globalRelation != null) {
                writeInheritedFormData(globalRelation, config.getGlobalBinding(),
                        formData, permissionMap, currentUserId, now);
            }
        }
    }

    @Override
    public TaskFormVO buildTaskForm(Integer processInstanceId, Integer processDefinitionId,
                                    String nodeId, boolean editable) {
        FormConfig config = resolveFormConfig(processDefinitionId, nodeId);
        if (config == null) {
            return null;
        }

        // 确定绑定来源
        ProcessDefinitionForm effectiveBinding = config.getNodeBinding() != null
                ? config.getNodeBinding()
                : config.getGlobalBinding();
        if (effectiveBinding == null) {
            return null;
        }

        // 查找关联记录
        ProcessInstanceForm relation = processInstanceFormMapper.selectOne(
                new LambdaQueryWrapper<ProcessInstanceForm>()
                        .eq(ProcessInstanceForm::getProcessInstanceId, processInstanceId)
                        .eq(ProcessInstanceForm::getNodeId, nodeId));
        if (relation == null) {
            return null;
        }

        // 构建权限 map
        Map<String, String> permissionMap = config.getFieldPermissions().stream()
                .collect(Collectors.toMap(
                        ProcessNodeFieldPermission::getFieldKey,
                        ProcessNodeFieldPermission::getPermission,
                        (a, b) -> b));

        // 组装 TaskFormVO
        TaskFormVO taskFormVO = new TaskFormVO();
        taskFormVO.setEditable(editable);
        taskFormVO.setFormId(effectiveBinding.getFormId());
        taskFormVO.setFormVersion(effectiveBinding.getFormVersion());
        taskFormVO.setFormInstanceId(relation.getFormInstanceId());

        // 构建字段和分组
        List<TaskFormFieldVO> formFields = new ArrayList<>();
        List<TaskFormGroupVO> groups = new ArrayList<>();
        buildFormFieldsAndGroups(
                effectiveBinding.getFormId(), effectiveBinding.getFormVersion(),
                relation.getFormInstanceId(), permissionMap, editable,
                null, formFields, groups);

        taskFormVO.setFormFields(formFields);
        taskFormVO.setGroups(groups);

        // 如果继承主表单，组装继承数据
        if (config.isInheritMainForm() && config.getGlobalBinding() != null
                && !config.getGlobalBinding().getFormId().equals(effectiveBinding.getFormId())) {
            taskFormVO.setInherited(buildInheritedForm(processInstanceId, config.getGlobalBinding(), editable));
        }

        return taskFormVO;
    }

    @Override
    public TaskFormVO buildStartForm(Integer processDefinitionId, String startNodeId) {
        FormConfig config = resolveFormConfig(processDefinitionId, startNodeId);
        if (config == null) {
            return null;
        }

        ProcessDefinitionForm effectiveBinding = config.getNodeBinding() != null
                ? config.getNodeBinding()
                : config.getGlobalBinding();
        if (effectiveBinding == null) {
            return null;
        }

        Map<String, String> permissionMap = config.getFieldPermissions().stream()
                .collect(Collectors.toMap(
                        ProcessNodeFieldPermission::getFieldKey,
                        ProcessNodeFieldPermission::getPermission,
                        (a, b) -> b));

        TaskFormVO taskFormVO = new TaskFormVO();
        taskFormVO.setEditable(true);
        taskFormVO.setFormId(effectiveBinding.getFormId());
        taskFormVO.setFormVersion(effectiveBinding.getFormVersion());
        taskFormVO.setFormInstanceId(null);

        List<TaskFormFieldVO> formFields = new ArrayList<>();
        List<TaskFormGroupVO> groups = new ArrayList<>();
        buildFormFieldsAndGroupsNoData(
                effectiveBinding.getFormId(), effectiveBinding.getFormVersion(),
                permissionMap, true, null, formFields, groups);

        taskFormVO.setFormFields(formFields);
        taskFormVO.setGroups(groups);

        // 继承主表单
        if (config.isInheritMainForm() && config.getGlobalBinding() != null
                && !config.getGlobalBinding().getFormId().equals(effectiveBinding.getFormId())) {
            ProcessDefinitionForm globalBinding = config.getGlobalBinding();
            TaskFormInheritedVO inheritedVO = new TaskFormInheritedVO();
            inheritedVO.setFormId(globalBinding.getFormId());
            inheritedVO.setFormVersion(globalBinding.getFormVersion());
            inheritedVO.setFormInstanceId(null);

            List<TaskFormFieldVO> inheritedFormFields = new ArrayList<>();
            List<TaskFormGroupVO> inheritedGroups = new ArrayList<>();
            buildFormFieldsAndGroupsNoData(
                    globalBinding.getFormId(), globalBinding.getFormVersion(),
                    Collections.emptyMap(), true, globalBinding.getFormId(),
                    inheritedFormFields, inheritedGroups);

            inheritedVO.setFormFields(inheritedFormFields);
            inheritedVO.setGroups(inheritedGroups);
            taskFormVO.setInherited(inheritedVO);
        }

        return taskFormVO;
    }

    /**
     * 构建字段和分组渲染数据（无实例值，仅模板配置）。用于发起前的表单展示。
     */
    private void buildFormFieldsAndGroupsNoData(String formId, String formVersion,
                                                 Map<String, String> permissionMap, boolean editable,
                                                 String sourceFormId,
                                                 List<TaskFormFieldVO> formFields, List<TaskFormGroupVO> groups) {
        List<DynamicFormField> fieldDefs = dynamicFormFieldMapper.selectList(
                new LambdaQueryWrapper<DynamicFormField>()
                        .eq(DynamicFormField::getFormId, formId)
                        .eq(DynamicFormField::getVersion, formVersion));

        List<DynamicFormFieldGroup> groupDefs = dynamicFormFieldGroupMapper.selectList(
                new LambdaQueryWrapper<DynamicFormFieldGroup>()
                        .eq(DynamicFormFieldGroup::getFormId, formId)
                        .eq(DynamicFormFieldGroup::getVersion, formVersion)
                        .orderByAsc(DynamicFormFieldGroup::getSort));

        Map<String, List<DynamicFormField>> fieldsByGroup = fieldDefs.stream()
                .collect(Collectors.groupingBy(f -> StringUtils.hasText(f.getGroupId()) ? f.getGroupId() : ""));

        List<DynamicFormField> ungroupedFields = fieldsByGroup.getOrDefault("", Collections.emptyList());
        for (DynamicFormField field : ungroupedFields) {
            formFields.add(toFieldVO(field, permissionMap, editable, null, sourceFormId));
        }

        for (DynamicFormFieldGroup groupDef : groupDefs) {
            List<DynamicFormField> groupedFields = fieldsByGroup.getOrDefault(groupDef.getId(), Collections.emptyList());
            if (groupedFields.isEmpty()) {
                continue;
            }

            List<TaskFormFieldVO> groupFieldVOs = new ArrayList<>();
            for (DynamicFormField field : groupedFields) {
                groupFieldVOs.add(toFieldVO(field, permissionMap, editable, null, sourceFormId));
            }

            TaskFormGroupVO groupVO = new TaskFormGroupVO();
            groupVO.setGroupId(groupDef.getId());
            groupVO.setName(groupDef.getName());
            groupVO.setDescription(groupDef.getDescription());
            groupVO.setSort(groupDef.getSort());
            groupVO.setCollapsed(groupDef.getCollapsed());
            groupVO.setFields(groupFieldVOs);
            groups.add(groupVO);
        }
    }

    // ========== 私有辅助方法 ==========

    /**
     * 解析表单配置：获取全局绑定、节点绑定、继承标记、节点字段权限。
     * 无任何绑定时返回 null。
     */
    private FormConfig resolveFormConfig(Integer processDefinitionId, String nodeId) {
        ProcessDefinition processDefinition = processDefinitionMapper.selectById(processDefinitionId);
        if (processDefinition == null) {
            return null;
        }

        // 已发布取 currentVersion，否则取 DRAFT
        String effectiveVersion;
        if ("1".equals(processDefinition.getStatus()) && StringUtils.hasText(processDefinition.getVersion())) {
            effectiveVersion = processDefinition.getVersion();
        } else {
            effectiveVersion = "DRAFT";
        }

        // 查询全局表单绑定
        ProcessDefinitionForm globalBinding = processDefinitionFormMapper.selectOne(
                new LambdaQueryWrapper<ProcessDefinitionForm>()
                        .eq(ProcessDefinitionForm::getProcessDefinitionId, processDefinitionId)
                        .eq(ProcessDefinitionForm::getVersion, effectiveVersion)
                        .eq(ProcessDefinitionForm::getBindType, "GLOBAL"));

        // 查询节点表单绑定
        ProcessDefinitionForm nodeBinding = null;
        boolean inheritMainForm = false;
        if (StringUtils.hasText(nodeId)) {
            nodeBinding = processDefinitionFormMapper.selectOne(
                    new LambdaQueryWrapper<ProcessDefinitionForm>()
                            .eq(ProcessDefinitionForm::getProcessDefinitionId, processDefinitionId)
                            .eq(ProcessDefinitionForm::getVersion, effectiveVersion)
                            .eq(ProcessDefinitionForm::getBindType, "NODE")
                            .eq(ProcessDefinitionForm::getNodeId, nodeId));

            if (nodeBinding != null && "1".equals(nodeBinding.getInheritMainForm())) {
                inheritMainForm = true;
            }
        }

        // 无任何绑定时返回 null
        if (globalBinding == null && nodeBinding == null) {
            return null;
        }

        // 查询节点字段权限 — 针对生效节点查询
        String effectiveNodeId = nodeId;
        List<ProcessNodeFieldPermission> fieldPermissions = Collections.emptyList();
        if (StringUtils.hasText(effectiveNodeId)) {
            fieldPermissions = processNodeFieldPermissionMapper.selectList(
                    new LambdaQueryWrapper<ProcessNodeFieldPermission>()
                            .eq(ProcessNodeFieldPermission::getProcessDefinitionId, processDefinitionId)
                            .eq(ProcessNodeFieldPermission::getVersion, effectiveVersion)
                            .eq(ProcessNodeFieldPermission::getNodeId, effectiveNodeId));
        }

        return new FormConfig(nodeBinding, globalBinding, inheritMainForm, fieldPermissions);
    }

    /**
     * 创建 DynamicFormInstance 并返回实例ID。
     */
    private String createDynamicFormInstance(String formId, String formVersion,
                                             String currentUserId, LocalDateTime now) {
        DynamicFormInstance instance = new DynamicFormInstance()
                .setFormId(formId)
                .setVersion(formVersion)
                .setCreateBy(currentUserId)
                .setCreateTime(now)
                .setUpdateTime(now);
        dynamicFormInstanceMapper.insert(instance);
        return instance.getId();
    }

    /**
     * 初始化表单实例的所有字段实例（空值行）。
     */
    private void initFieldInstances(String formInstanceId, String formId, String formVersion,
                                    String currentUserId, LocalDateTime now) {
        List<DynamicFormField> fields = dynamicFormFieldMapper.selectList(
                new LambdaQueryWrapper<DynamicFormField>()
                        .eq(DynamicFormField::getFormId, formId)
                        .eq(DynamicFormField::getVersion, formVersion));

        for (DynamicFormField field : fields) {
            DynamicFormFieldInstance fieldInstance = new DynamicFormFieldInstance()
                    .setFormFieldId(field.getId())
                    .setFormInstanceId(formInstanceId)
                    .setVal(null)
                    .setVersion(formVersion)
                    .setCreateBy(currentUserId)
                    .setCreateTime(now)
                    .setUpdateTime(now);
            dynamicFormFieldInstanceMapper.insert(fieldInstance);
        }
    }

    /**
     * 构建字段和分组渲染数据。
     *
     * @param formId         表单ID
     * @param formVersion    表单版本
     * @param formInstanceId 表单实例ID
     * @param permissionMap  字段权限 map
     * @param editable       是否可编辑
     * @param sourceFormId   来源表单ID（仅继承字段有值）
     * @param formFields     输出：未分组字段列表
     * @param groups         输出：分组字段列表
     */
    private void buildFormFieldsAndGroups(String formId, String formVersion, String formInstanceId,
                                          Map<String, String> permissionMap, boolean editable,
                                          String sourceFormId,
                                          List<TaskFormFieldVO> formFields, List<TaskFormGroupVO> groups) {
        // 加载字段定义
        List<DynamicFormField> fieldDefs = dynamicFormFieldMapper.selectList(
                new LambdaQueryWrapper<DynamicFormField>()
                        .eq(DynamicFormField::getFormId, formId)
                        .eq(DynamicFormField::getVersion, formVersion));

        // 加载字段实例值
        List<DynamicFormFieldInstance> fieldInstances = dynamicFormFieldInstanceMapper.selectList(
                new LambdaQueryWrapper<DynamicFormFieldInstance>()
                        .eq(DynamicFormFieldInstance::getFormInstanceId, formInstanceId));

        // 构建 DB主键id -> 实例值 的映射
        Map<String, Object> dbIdToValue = fieldInstances.stream()
                .collect(Collectors.toMap(
                        DynamicFormFieldInstance::getFormFieldId,
                        DynamicFormFieldInstance::getVal,
                        (a, b) -> b));

        // 加载分组
        List<DynamicFormFieldGroup> groupDefs = dynamicFormFieldGroupMapper.selectList(
                new LambdaQueryWrapper<DynamicFormFieldGroup>()
                        .eq(DynamicFormFieldGroup::getFormId, formId)
                        .eq(DynamicFormFieldGroup::getVersion, formVersion)
                        .orderByAsc(DynamicFormFieldGroup::getSort));

        // 按 groupId 分组字段
        Map<String, List<DynamicFormField>> fieldsByGroup = fieldDefs.stream()
                .collect(Collectors.groupingBy(f -> StringUtils.hasText(f.getGroupId()) ? f.getGroupId() : ""));

        // 处理未分组字段
        List<DynamicFormField> ungroupedFields = fieldsByGroup.getOrDefault("", Collections.emptyList());
        for (DynamicFormField field : ungroupedFields) {
            Object value = dbIdToValue.get(field.getId());
            formFields.add(toFieldVO(field, permissionMap, editable, value, sourceFormId));
        }

        // 处理分组字段
        for (DynamicFormFieldGroup groupDef : groupDefs) {
            List<DynamicFormField> groupedFields = fieldsByGroup.getOrDefault(groupDef.getId(), Collections.emptyList());
            if (groupedFields.isEmpty()) {
                continue;
            }

            List<TaskFormFieldVO> groupFieldVOs = new ArrayList<>();
            for (DynamicFormField field : groupedFields) {
                Object value = dbIdToValue.get(field.getId());
                groupFieldVOs.add(toFieldVO(field, permissionMap, editable, value, sourceFormId));
            }

            TaskFormGroupVO groupVO = new TaskFormGroupVO();
            groupVO.setGroupId(groupDef.getId());
            groupVO.setName(groupDef.getName());
            groupVO.setDescription(groupDef.getDescription());
            groupVO.setSort(groupDef.getSort());
            groupVO.setCollapsed(groupDef.getCollapsed());
            groupVO.setFields(groupFieldVOs);
            groups.add(groupVO);
        }
    }

    /**
     * 将 DynamicFormField 转换为 TaskFormFieldVO。
     */
    private TaskFormFieldVO toFieldVO(DynamicFormField field, Map<String, String> permissionMap,
                                      boolean editable, Object value, String sourceFormId) {
        String fieldKey = field.getFieldId();
        String permission = permissionMap.getOrDefault(fieldKey, "VISIBLE");

        // 不可编辑时强制 READONLY
        if (!editable) {
            permission = "READONLY";
        }

        boolean required = "REQUIRED".equals(permission) || "1".equals(field.getRequired());

        TaskFormFieldVO vo = new TaskFormFieldVO();
        vo.setFieldKey(fieldKey);
        vo.setLabel(field.getTitle());
        vo.setType(field.getType().name());
        vo.setPermission(permission);
        vo.setValue(value);
        vo.setRequired(required);
        vo.setOptions(field.getOptions());
        vo.setSourceFormId(sourceFormId);
        return vo;
    }

    /**
     * 构建继承的主表单渲染数据。
     */
    private TaskFormInheritedVO buildInheritedForm(Integer processInstanceId,
                                                   ProcessDefinitionForm globalBinding,
                                                   boolean editable) {
        // 查找全局表单关联记录
        ProcessInstanceForm globalRelation = processInstanceFormMapper.selectOne(
                new LambdaQueryWrapper<ProcessInstanceForm>()
                        .eq(ProcessInstanceForm::getProcessInstanceId, processInstanceId)
                        .isNull(ProcessInstanceForm::getNodeId));
        if (globalRelation == null) {
            return null;
        }

        TaskFormInheritedVO inheritedVO = new TaskFormInheritedVO();
        inheritedVO.setFormId(globalBinding.getFormId());
        inheritedVO.setFormVersion(globalBinding.getFormVersion());
        inheritedVO.setFormInstanceId(globalRelation.getFormInstanceId());

        // 全局表单的权限 map 为空（继承字段默认 VISIBLE）
        Map<String, String> emptyPermissionMap = Collections.emptyMap();

        List<TaskFormFieldVO> formFields = new ArrayList<>();
        List<TaskFormGroupVO> groups = new ArrayList<>();
        buildFormFieldsAndGroups(
                globalBinding.getFormId(), globalBinding.getFormVersion(),
                globalRelation.getFormInstanceId(), emptyPermissionMap, editable,
                globalBinding.getFormId(), formFields, groups);

        inheritedVO.setFormFields(formFields);
        inheritedVO.setGroups(groups);
        return inheritedVO;
    }

    /**
     * 向全局（继承）表单实例写入适用的字段数据。
     */
    private void writeInheritedFormData(ProcessInstanceForm globalRelation,
                                        ProcessDefinitionForm globalBinding,
                                        Map<String, Object> formData,
                                        Map<String, String> nodePermissionMap,
                                        String currentUserId, LocalDateTime now) {
        // 加载全局表单的字段定义
        List<DynamicFormField> globalFieldDefs = dynamicFormFieldMapper.selectList(
                new LambdaQueryWrapper<DynamicFormField>()
                        .eq(DynamicFormField::getFormId, globalBinding.getFormId())
                        .eq(DynamicFormField::getVersion, globalBinding.getFormVersion()));

        // 全局字段 fieldKey -> DynamicFormField
        Map<String, DynamicFormField> globalFieldKeyMap = globalFieldDefs.stream()
                .collect(Collectors.toMap(DynamicFormField::getFieldId, f -> f, (a, b) -> b));

        // 全局字段 fieldKey -> DB主键id
        Map<String, String> globalFieldIdToDbId = globalFieldDefs.stream()
                .collect(Collectors.toMap(DynamicFormField::getFieldId, DynamicFormField::getId, (a, b) -> b));

        // 加载已有字段实例
        List<DynamicFormFieldInstance> existingInstances = dynamicFormFieldInstanceMapper.selectList(
                new LambdaQueryWrapper<DynamicFormFieldInstance>()
                        .eq(DynamicFormFieldInstance::getFormInstanceId, globalRelation.getFormInstanceId()));

        Map<String, DynamicFormFieldInstance> dbIdToInstance = existingInstances.stream()
                .collect(Collectors.toMap(DynamicFormFieldInstance::getFormFieldId, f -> f, (a, b) -> b));

        List<DynamicFormFieldInstance> toInsert = new ArrayList<>();
        List<DynamicFormFieldInstance> toUpdate = new ArrayList<>();

        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            String fieldKey = entry.getKey();
            Object value = entry.getValue();

            DynamicFormField fieldDef = globalFieldKeyMap.get(fieldKey);
            if (fieldDef == null) {
                continue; // 非全局表单字段，跳过
            }

            // 节点权限为 HIDDEN 的字段不写入全局
            String nodePermission = nodePermissionMap.getOrDefault(fieldKey, "VISIBLE");
            if ("HIDDEN".equals(nodePermission)) {
                continue;
            }

            String dbId = globalFieldIdToDbId.get(fieldKey);
            if (dbId == null) {
                continue;
            }

            DynamicFormFieldInstance instance = dbIdToInstance.get(dbId);
            if (instance != null) {
                instance.setVal(value);
                instance.setUpdateTime(now);
                toUpdate.add(instance);
            } else {
                DynamicFormFieldInstance newInstance = new DynamicFormFieldInstance()
                        .setFormFieldId(dbId)
                        .setFormInstanceId(globalRelation.getFormInstanceId())
                        .setVal(value)
                        .setVersion(globalBinding.getFormVersion())
                        .setCreateBy(currentUserId)
                        .setCreateTime(now);
                toInsert.add(newInstance);
            }
        }

        if (!toInsert.isEmpty()) {
            for (DynamicFormFieldInstance inst : toInsert) {
                dynamicFormFieldInstanceMapper.insert(inst);
            }
        }
        if (!toUpdate.isEmpty()) {
            for (DynamicFormFieldInstance inst : toUpdate) {
                dynamicFormFieldInstanceMapper.updateById(inst);
            }
        }
    }

    /**
     * 判断值是否为非空（用于必填校验）。
     */
    private boolean hasValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof CharSequence) {
            return StringUtils.hasText((CharSequence) value);
        }
        if (value instanceof Collection) {
            return !((Collection<?>) value).isEmpty();
        }
        if (value instanceof Map) {
            return !((Map<?, ?>) value).isEmpty();
        }
        return true;
    }
}