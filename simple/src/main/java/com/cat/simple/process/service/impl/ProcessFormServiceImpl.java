package com.cat.simple.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.dynamicForm.*;
import com.cat.common.entity.process.*;
import com.cat.common.entity.process.constants.FieldPermissionConstants;
import com.cat.common.entity.process.constants.FormBindType;
import com.cat.simple.config.flowable.guard.ProcessGuard;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.form.mapper.*;
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
import org.flowable.task.api.Task;
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
    @Resource
    private DynamicFormMapper dynamicFormMapper;
    @Resource
    private DynamicFormPublishHistoryMapper dynamicFormPublishHistoryMapper;

    // ========== 表单配置内部类 ==========

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class FormConfig {
        private ProcessDefinitionForm globalBinding;
        private List<ProcessNodeFieldPermission> globalFieldPermissions;
        private boolean inheritMainForm;
        private ProcessDefinitionForm nodeBinding;

    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDraftBindings(Integer processDefinitionId,
                                  ProcessDefinitionForm globalFormBinding,
                                  List<ProcessDefinitionForm> nodeFormBindings,
                                  List<ProcessNodeFieldPermission> nodeFieldPermissions) {
        // 1. 全局表单绑定
        processDefinitionFormMapper.delete(
                new LambdaQueryWrapper<ProcessDefinitionForm>()
                        .eq(ProcessDefinitionForm::getProcessDefinitionId, processDefinitionId)
                        .eq(ProcessDefinitionForm::getVersion, "DRAFT")
                        .eq(ProcessDefinitionForm::getBindType, FormBindType.GLOBAL));
        if (globalFormBinding != null && StringUtils.hasText(globalFormBinding.getFormId())) {
            validateFormBinding(globalFormBinding);
            globalFormBinding.setProcessDefinitionId(processDefinitionId);
            globalFormBinding.setVersion("DRAFT");
            globalFormBinding.setBindType(FormBindType.GLOBAL);
            globalFormBinding.setNodeId(null);
            globalFormBinding.setCreateTime(LocalDateTime.now());
            processDefinitionFormMapper.insert(globalFormBinding);
        }

        // 2. 节点表单绑定
        processDefinitionFormMapper.delete(
                new LambdaQueryWrapper<ProcessDefinitionForm>()
                        .eq(ProcessDefinitionForm::getProcessDefinitionId, processDefinitionId)
                        .eq(ProcessDefinitionForm::getVersion, "DRAFT")
                        .eq(ProcessDefinitionForm::getBindType, FormBindType.NODE));
        if (!CollectionUtils.isEmpty(nodeFormBindings)) {
            for (ProcessDefinitionForm binding : nodeFormBindings) {
                // TODO 节点表单绑定 验证 绑定
//                validateFormBinding(binding);
                binding.setFormId(null);
                binding.setFormVersion(null);
                binding.setProcessDefinitionId(processDefinitionId);
                binding.setVersion("DRAFT");
                binding.setBindType(FormBindType.NODE);
                binding.setCreateTime(LocalDateTime.now());
                processDefinitionFormMapper.insert(binding);
            }
        }

        // 3. 节点字段权限
        processNodeFieldPermissionMapper.delete(
                new LambdaQueryWrapper<ProcessNodeFieldPermission>()
                        .eq(ProcessNodeFieldPermission::getProcessDefinitionId, processDefinitionId)
                        .eq(ProcessNodeFieldPermission::getVersion, "DRAFT"));
        if (!CollectionUtils.isEmpty(nodeFieldPermissions)) {
            for (ProcessNodeFieldPermission permission : nodeFieldPermissions) {
                permission.setProcessDefinitionId(processDefinitionId);
                permission.setVersion("DRAFT");
                permission.setCreateTime(LocalDateTime.now());
                processNodeFieldPermissionMapper.insert(permission);
            }
        }
    }

    @Override
    public TaskFormVO buildTaskFormByNodeId(Integer processDefinitionId, String processVersion, String nodeId) {

        FormConfig formConfig = getFormConfig(processDefinitionId, processVersion, nodeId);

        if(Objects.isNull(formConfig) || !formConfig.isInheritMainForm()){
            return null;
        }

        TaskFormVO taskFormVO = new TaskFormVO();

        DynamicForm dynamicForm = loadDynamicForm(formConfig.getGlobalBinding().getFormId(), formConfig.getGlobalBinding().getFormVersion(), formConfig.getGlobalFieldPermissions());
        taskFormVO.setGlobalForm(dynamicForm);

        return taskFormVO;
    }

    @Override
    public TaskFormVO buildTaskFormByNodeIdWithData(Integer processDefinitionId, String processVersion, Integer processInstanceId, String nodeId) {


        FormConfig formConfig = getFormConfig(processDefinitionId, processVersion, nodeId);

        if(Objects.isNull(formConfig) || !formConfig.isInheritMainForm()){
            return null;
        }
//
        ProcessInstanceForm processInstanceForm = processInstanceFormMapper
                .selectOne(new LambdaQueryWrapper<ProcessInstanceForm>().eq(ProcessInstanceForm::getProcessInstanceId, processInstanceId).isNull(ProcessInstanceForm::getNodeId));
        if(Objects.isNull(processInstanceForm) || !StringUtils.hasText(processInstanceForm.getFormInstanceId())){
            return null;
        }

        TaskFormVO taskFormVO = new TaskFormVO();
        DynamicForm form = dynamicFormService.infoInstance(processInstanceForm.getFormInstanceId());
        loadPermission(form, formConfig.getGlobalFieldPermissions());
        taskFormVO.setGlobalForm(form);

        return taskFormVO;
    }

    @Override
    public void writeFormData(ProcessInstance instance, Map<String, Object> formRawData) {
        FormData formData = new FormData();

        formData.setData(formRawData);

        String currentUserId = Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId();

        ProcessInstanceForm processInstanceForm = processInstanceFormMapper
                .selectOne(new LambdaQueryWrapper<ProcessInstanceForm>().eq(ProcessInstanceForm::getProcessInstanceId, instance.getId()).isNull(ProcessInstanceForm::getNodeId));
        if(Objects.nonNull(processInstanceForm)){
            formData.setFormInstanceId(processInstanceForm.getFormInstanceId());

            dynamicFormService.saveFormData(formData, currentUserId);
        }else {

            ProcessDefinitionForm globalBinding = getGlobalBinding(instance.getProcessDefinitionId(), instance.getProcessDefinitionVersion());
            if(Objects.isNull(globalBinding)){
                return;
            }
            formData.setFormId(globalBinding.getFormId());
            formData.setVersion(globalBinding.getFormVersion());

            String formInstanceId = dynamicFormService.saveFormData(formData, Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId());


            ProcessInstanceForm relation = new ProcessInstanceForm()
                    .setProcessInstanceId(instance.getId())
                    .setFormId(formData.getFormId())
                    .setFormVersion(formData.getVersion())
                    .setFormInstanceId(formInstanceId)
                    .setCreateBy(currentUserId)
                    .setCreateTime(LocalDateTime.now());
            processInstanceFormMapper.insert(relation);
        }

    }

    @Override
    public List<DynamicFormField> getGlobalFields(Integer processInstanceId) {
        ProcessInstanceForm processInstanceForm = processInstanceFormMapper.selectOne(new LambdaQueryWrapper<ProcessInstanceForm>().eq(ProcessInstanceForm::getProcessInstanceId, processInstanceId));
        if(StringUtils.hasText(processInstanceForm.getFormInstanceId())){
            return dynamicFormService.allFieldInstance(processInstanceForm.getFormInstanceId());
        }
        return List.of();
    }

    private DynamicForm loadDynamicForm(String formId, String formVersion, List<ProcessNodeFieldPermission> permissions){

        boolean exists = dynamicFormPublishHistoryMapper.exists(new LambdaQueryWrapper<DynamicFormPublishHistory>()
                .eq(DynamicFormPublishHistory::getFormId, formId).eq(DynamicFormPublishHistory::getVersion, formVersion));
        if(!exists){
            throw new IllegalStateException("绑定表单或版本不存在");
        }

        DynamicForm form = new DynamicForm();
        form.setId(formId);
        form.setVersion(formVersion);
        form = dynamicFormService.info(form);


        return loadPermission(form, permissions);
    }

    private DynamicForm loadPermission(DynamicForm form,List<ProcessNodeFieldPermission> permissions){
        if(Objects.isNull(permissions)){
            permissions = new ArrayList<>();
        }

        Map<String, String> permissionMap = permissions.stream()
                .collect(Collectors.toMap(
                        ProcessNodeFieldPermission::getFieldKey,
                        ProcessNodeFieldPermission::getPermission,
                        (a, b) -> b));

        List<DynamicFormField> allFields = new ArrayList<>();
        if (form.getFields() != null) {
            allFields.addAll(form.getFields());
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
            String permission = permissionMap.getOrDefault(fieldKey, FieldPermissionConstants.VISIBLE);
            field.setPermission(permission);
        }

        return form;
    }

    private FormConfig getFormConfig(Integer processDefinitionId, String processVersion, String nodeId){

        // 查询节点配置
        ProcessDefinitionForm nodeBinding = processDefinitionFormMapper.selectOne(
                new LambdaQueryWrapper<ProcessDefinitionForm>()
                        .eq(ProcessDefinitionForm::getProcessDefinitionId, processDefinitionId)
                        .eq(ProcessDefinitionForm::getVersion, processVersion)
                        .eq(ProcessDefinitionForm::getBindType, FormBindType.NODE)
                        .eq(ProcessDefinitionForm::getNodeId, nodeId));

        if(Objects.nonNull(nodeBinding) && nodeBinding.getInheritMainForm().equals("1")){
            FormConfig formConfig = new FormConfig();
            formConfig.setInheritMainForm(true);
            // 查询全局表单
            ProcessDefinitionForm globalBinding = processDefinitionFormMapper.selectOne(
                    new LambdaQueryWrapper<ProcessDefinitionForm>()
                            .eq(ProcessDefinitionForm::getProcessDefinitionId, processDefinitionId)
                            .eq(ProcessDefinitionForm::getVersion, processVersion)
                            .eq(ProcessDefinitionForm::getBindType, FormBindType.GLOBAL));
            if(Objects.isNull(processVersion)){
                // TODO 后续节点自绑定表单 移除此处判定
                return null;
            }
            formConfig.setGlobalBinding(globalBinding);
            formConfig.setNodeBinding(nodeBinding);

            List<ProcessNodeFieldPermission> processNodeFieldPermissions = processNodeFieldPermissionMapper.selectList(
                    new LambdaQueryWrapper<ProcessNodeFieldPermission>()
                            .eq(ProcessNodeFieldPermission::getProcessDefinitionId, processDefinitionId)
                            .eq(ProcessNodeFieldPermission::getVersion, processVersion)
                            .eq(ProcessNodeFieldPermission::getNodeId, nodeId));

            formConfig.setGlobalFieldPermissions(processNodeFieldPermissions);
            return formConfig;
        }else {
            // TODO 续节点自绑定表单 移除此处判定
            return null;
        }


    }

    private ProcessDefinitionForm getGlobalBinding(Integer processDefinitionId, String processVersion){
        return processDefinitionFormMapper.selectOne(
                new LambdaQueryWrapper<ProcessDefinitionForm>()
                        .eq(ProcessDefinitionForm::getProcessDefinitionId, processDefinitionId)
                        .eq(ProcessDefinitionForm::getVersion, processVersion)
                        .eq(ProcessDefinitionForm::getBindType, FormBindType.GLOBAL));
    }

    /**
     * 验证表单绑定：禁止绑定 DRAFT 版本，且表单及版本必须真实存在。
     */
    private void validateFormBinding(ProcessDefinitionForm binding) {
        if (binding == null || !StringUtils.hasText(binding.getFormId())) {
            return;
        }
        String formVersion = binding.getFormVersion();
        if (!StringUtils.hasText(formVersion)) {
            throw new IllegalArgumentException("表单版本不能为空");
        }
        if ("DRAFT".equals(formVersion)) {
            throw new IllegalArgumentException("禁止绑定DRAFT版本表单");
        }
        DynamicForm form = dynamicFormMapper.selectById(binding.getFormId());
        if (form == null) {
            throw new IllegalArgumentException("绑定的表单不存在: " + binding.getFormId());
        }
        long count = dynamicFormFieldMapper.selectCount(
                new LambdaQueryWrapper<DynamicFormField>()
                        .eq(DynamicFormField::getFormId, binding.getFormId())
                        .eq(DynamicFormField::getVersion, formVersion));
        if (count == 0) {
            throw new IllegalArgumentException("绑定的表单版本不存在: " + binding.getFormId() + "@" + formVersion);
        }
    }




}