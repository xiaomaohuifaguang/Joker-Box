package com.cat.simple.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.dynamicForm.*;
import com.cat.common.entity.process.*;
import com.cat.simple.config.flowable.guard.ProcessGuard;
import com.cat.simple.form.mapper.DynamicFormFieldInstanceMapper;
import com.cat.simple.form.mapper.DynamicFormFieldMapper;
import com.cat.simple.form.mapper.DynamicFormInstanceMapper;
import com.cat.simple.form.service.DynamicFormService;
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
    private ProcessDefinitionMapper processDefinitionMapper;
    @Resource
    private ProcessGuard guard;
    @Resource
    private DynamicFormService dynamicFormService;

    // ========== 表单配置内部类 ==========

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class FormConfig {
        private ProcessDefinitionForm nodeBinding;
        private ProcessDefinitionForm globalBinding;
        private boolean inheritMainForm;
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

        ProcessDefinitionForm effectiveBinding = config.getNodeBinding() != null
                ? config.getNodeBinding()
                : config.getGlobalBinding();
        if (effectiveBinding == null) {
            return null;
        }

        ProcessInstanceForm existingRelation = processInstanceFormMapper.selectOne(
                new LambdaQueryWrapper<ProcessInstanceForm>()
                        .eq(ProcessInstanceForm::getProcessInstanceId, processInstanceId)
                        .eq(ProcessInstanceForm::getNodeId, nodeId));
        if (existingRelation != null) {
            return existingRelation;
        }

        String currentUserId = guard.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        String formInstanceId = createDynamicFormInstance(
                effectiveBinding.getFormId(), effectiveBinding.getFormVersion(), currentUserId, now);
        initFieldInstances(formInstanceId, effectiveBinding.getFormId(),
                effectiveBinding.getFormVersion(), currentUserId, now);

        ProcessInstanceForm relation = new ProcessInstanceForm()
                .setProcessInstanceId(processInstanceId)
                .setNodeId(nodeId)
                .setFormId(effectiveBinding.getFormId())
                .setFormVersion(effectiveBinding.getFormVersion())
                .setFormInstanceId(formInstanceId)
                .setCreateBy(currentUserId)
                .setCreateTime(now);
        processInstanceFormMapper.insert(relation);

        if (config.isInheritMainForm() && config.getGlobalBinding() != null
                && !config.getGlobalBinding().getFormId().equals(effectiveBinding.getFormId())) {
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
                              String nodeId, Map<String, Object> nodeFormData,
                              Map<String, Object> globalFormData, boolean skipRequired) {
        FormConfig config = resolveFormConfig(processDefinitionId, nodeId);
        if (config == null) {
            return;
        }

        Map<String, String> permissionMap = config.getFieldPermissions().stream()
                .collect(Collectors.toMap(
                        ProcessNodeFieldPermission::getFieldKey,
                        ProcessNodeFieldPermission::getPermission,
                        (a, b) -> b));

        // 1. 节点表单数据
        if (!CollectionUtils.isEmpty(nodeFormData)) {
            ProcessDefinitionForm effectiveBinding = config.getNodeBinding() != null
                    ? config.getNodeBinding()
                    : config.getGlobalBinding();
            if (effectiveBinding != null) {
                ProcessInstanceForm relation = processInstanceFormMapper.selectOne(
                        new LambdaQueryWrapper<ProcessInstanceForm>()
                                .eq(ProcessInstanceForm::getProcessInstanceId, processInstanceId)
                                .eq(ProcessInstanceForm::getNodeId, nodeId));
                if (relation != null) {
                    FormData data = new FormData();
                    data.setFormId(effectiveBinding.getFormId());
                    data.setVersion(effectiveBinding.getFormVersion());
                    data.setFormInstanceId(relation.getFormInstanceId());
                    data.setData(filterWritable(nodeFormData, permissionMap));
                    dynamicFormService.saveFormData(data, skipRequired);
                }
            }
        }

        // 2. 全局表单数据（仅当继承全局且全局表单与节点表单不同时）
        if (!CollectionUtils.isEmpty(globalFormData) && config.isInheritMainForm()
                && config.getGlobalBinding() != null
                && (config.getNodeBinding() == null
                    || !config.getGlobalBinding().getFormId().equals(config.getNodeBinding().getFormId()))) {
            ProcessInstanceForm globalRelation = processInstanceFormMapper.selectOne(
                    new LambdaQueryWrapper<ProcessInstanceForm>()
                            .eq(ProcessInstanceForm::getProcessInstanceId, processInstanceId)
                            .isNull(ProcessInstanceForm::getNodeId));
            if (globalRelation != null) {
                FormData data = new FormData();
                data.setFormId(config.getGlobalBinding().getFormId());
                data.setVersion(config.getGlobalBinding().getFormVersion());
                data.setFormInstanceId(globalRelation.getFormInstanceId());
                data.setData(filterWritable(globalFormData, permissionMap));
                dynamicFormService.saveFormData(data, skipRequired);
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

        ProcessDefinitionForm effectiveBinding = config.getNodeBinding() != null
                ? config.getNodeBinding()
                : config.getGlobalBinding();
        if (effectiveBinding == null) {
            return null;
        }

        ProcessInstanceForm relation;
        if (config.getNodeBinding() != null) {
            relation = processInstanceFormMapper.selectOne(
                    new LambdaQueryWrapper<ProcessInstanceForm>()
                            .eq(ProcessInstanceForm::getProcessInstanceId, processInstanceId)
                            .eq(ProcessInstanceForm::getNodeId, nodeId));
            if (relation == null) {
                relation = createFormInstanceIfNeeded(processInstanceId, processDefinitionId, nodeId);
            }
        } else {
            relation = processInstanceFormMapper.selectOne(
                    new LambdaQueryWrapper<ProcessInstanceForm>()
                            .eq(ProcessInstanceForm::getProcessInstanceId, processInstanceId)
                            .isNull(ProcessInstanceForm::getNodeId));
        }
        if (relation == null) {
            return null;
        }

        Map<String, String> permissionMap = config.getFieldPermissions().stream()
                .collect(Collectors.toMap(
                        ProcessNodeFieldPermission::getFieldKey,
                        ProcessNodeFieldPermission::getPermission,
                        (a, b) -> b));

        DynamicForm nodeForm = loadFormWithData(
                effectiveBinding.getFormId(), effectiveBinding.getFormVersion(),
                relation.getFormInstanceId(), permissionMap, editable);

        TaskFormVO taskFormVO = new TaskFormVO();
        taskFormVO.setEditable(editable);
        taskFormVO.setNodeForm(nodeForm);

        if (config.isInheritMainForm() && config.getGlobalBinding() != null
                && !config.getGlobalBinding().getFormId().equals(effectiveBinding.getFormId())) {
            ProcessInstanceForm globalRelation = processInstanceFormMapper.selectOne(
                    new LambdaQueryWrapper<ProcessInstanceForm>()
                            .eq(ProcessInstanceForm::getProcessInstanceId, processInstanceId)
                            .isNull(ProcessInstanceForm::getNodeId));
            if (globalRelation != null) {
                DynamicForm globalForm = loadFormWithData(
                        config.getGlobalBinding().getFormId(),
                        config.getGlobalBinding().getFormVersion(),
                        globalRelation.getFormInstanceId(),
                        Collections.emptyMap(), editable);
                taskFormVO.setGlobalForm(globalForm);
            }
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

        DynamicForm nodeForm = loadFormWithData(
                effectiveBinding.getFormId(), effectiveBinding.getFormVersion(),
                null, permissionMap, true);

        TaskFormVO taskFormVO = new TaskFormVO();
        taskFormVO.setEditable(true);
        taskFormVO.setNodeForm(nodeForm);

        if (config.isInheritMainForm() && config.getGlobalBinding() != null
                && !config.getGlobalBinding().getFormId().equals(effectiveBinding.getFormId())) {
            DynamicForm globalForm = loadFormWithData(
                    config.getGlobalBinding().getFormId(),
                    config.getGlobalBinding().getFormVersion(),
                    null, Collections.emptyMap(), true);
            taskFormVO.setGlobalForm(globalForm);
        }

        return taskFormVO;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 加载表单模板并回填权限和实例值。
     */
    private DynamicForm loadFormWithData(String formId, String formVersion, String formInstanceId,
                                         Map<String, String> permissionMap, boolean editable) {
        DynamicForm form = loadFormTemplate(formId, formVersion);
        if (form == null) {
            return null;
        }
        form.setFormInstanceId(formInstanceId);

        Map<String, Object> dbIdToValue = Collections.emptyMap();
        if (StringUtils.hasText(formInstanceId)) {
            List<DynamicFormFieldInstance> fieldInstances = dynamicFormFieldInstanceMapper.selectList(
                    new LambdaQueryWrapper<DynamicFormFieldInstance>()
                            .eq(DynamicFormFieldInstance::getFormInstanceId, formInstanceId));
            dbIdToValue = fieldInstances.stream()
                    .filter(f -> f.getVal() != null)
                    .collect(Collectors.toMap(
                            DynamicFormFieldInstance::getFormFieldId,
                            DynamicFormFieldInstance::getVal,
                            (a, b) -> b));
        }

        List<DynamicFormField> allFields = new ArrayList<>();
        if (form.getFormFields() != null) {
            allFields.addAll(form.getFormFields());
        }
        if (form.getGroups() != null) {
            for (DynamicFormFieldGroup group : form.getGroups()) {
                if (group.getFields() != null) {
                    allFields.addAll(group.getFields());
                }
            }
        }

        for (DynamicFormField field : allFields) {
            String fieldKey = field.getFieldId();
            String permission = permissionMap.getOrDefault(fieldKey, "VISIBLE");
            if (!editable) {
                permission = "READONLY";
            }
            field.setPermission(permission);

            Object value = dbIdToValue.get(field.getId());
            field.setValue(value != null ? value : field.getDefaultValue());
        }

        return form;
    }

    /**
     * 加载表单模板（字段 + 分组 + 联动规则）。
     */
    private DynamicForm loadFormTemplate(String formId, String formVersion) {
        DynamicForm param = new DynamicForm();
        param.setId(formId);
        param.setVersion(formVersion);
        return dynamicFormService.info(param);
    }

    private FormConfig resolveFormConfig(Integer processDefinitionId, String nodeId) {
        ProcessDefinition processDefinition = processDefinitionMapper.selectById(processDefinitionId);
        if (processDefinition == null) {
            return null;
        }

        String effectiveVersion;
        if ("1".equals(processDefinition.getStatus()) && StringUtils.hasText(processDefinition.getVersion())) {
            effectiveVersion = processDefinition.getVersion();
        } else {
            effectiveVersion = "DRAFT";
        }

        ProcessDefinitionForm globalBinding = processDefinitionFormMapper.selectOne(
                new LambdaQueryWrapper<ProcessDefinitionForm>()
                        .eq(ProcessDefinitionForm::getProcessDefinitionId, processDefinitionId)
                        .eq(ProcessDefinitionForm::getVersion, effectiveVersion)
                        .eq(ProcessDefinitionForm::getBindType, "GLOBAL"));

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

        if (globalBinding == null && nodeBinding == null) {
            return null;
        }

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

    private Map<String, Object> filterWritable(Map<String, Object> formData, Map<String, String> permissionMap) {
        Map<String, Object> filtered = new HashMap<>();
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            String permission = permissionMap.getOrDefault(entry.getKey(), "VISIBLE");
            if (!"READONLY".equals(permission) && !"HIDDEN".equals(permission)) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

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