package com.cat.simple.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.dynamicForm.*;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.mapper.DynamicFormFieldInstanceMapper;
import com.cat.simple.mapper.DynamicFormFieldMapper;
import com.cat.simple.mapper.DynamicFormInstanceMapper;
import com.cat.simple.mapper.DynamicFormMapper;
import com.cat.simple.service.DynamicFormService;
import jakarta.annotation.Resource;
import org.apache.ibatis.executor.BatchResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @Override
    @Transactional
    public boolean add(DynamicForm dynamicForm){
        if(dynamicFormMapper.exists(new LambdaQueryWrapper<DynamicForm>().eq(DynamicForm::getId, dynamicForm.getId()))) {
            return false;
        }
        dynamicForm.setId(null);
        dynamicForm.setDeleted("0");
        dynamicForm.setVersion("1");
        dynamicForm.setStatus("0"); // 0 草稿 1 发布
        dynamicForm.setCreateBy(Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId());
        dynamicForm.setCreateTime(LocalDateTime.now());
        dynamicForm.setUpdateTime(dynamicForm.getCreateTime());
        int insert = dynamicFormMapper.insert(dynamicForm);

        if(!CollectionUtils.isEmpty(dynamicForm.getFormFields())){

            // 判断是否重复fieldId
            List<String> list = dynamicForm.getFormFields().stream().map(DynamicFormField::getFieldId).toList();
            if(list.size() != dynamicForm.getFormFields().size()){
                return false;
            }
            dynamicForm.getFormFields().forEach(formField -> {
                formField.setId(null);
                formField.setDeleted("0");
                formField.setFormId(String.valueOf(dynamicForm.getId()));
                formField.setVersion(dynamicForm.getVersion());
                formField.setCreateBy(dynamicForm.getCreateBy());
                formField.setCreateTime(dynamicForm.getUpdateTime());
                formField.setUpdateTime(dynamicForm.getUpdateTime());
            });
            dynamicFormFieldMapper.insert(dynamicForm.getFormFields());
        }


        return insert == 1;
    }

    @Override
    @Transactional
    public boolean delete(DynamicForm dynamicForm){
        dynamicForm = dynamicFormMapper.selectById(dynamicForm.getId());
        if(!dynamicForm.getStatus().equals("0")){
            return false;
        }
        dynamicFormFieldMapper.delete(new LambdaQueryWrapper<DynamicFormField>().eq(DynamicFormField::getFormId, dynamicForm.getId()));

         return dynamicFormMapper.deleteById(dynamicForm) == 1;
    }

    @Override
    public boolean update(DynamicForm dynamicForm){
        DynamicForm oriForm = dynamicFormMapper.selectById(dynamicForm.getId());
        if(oriForm == null) {
            return false;
        }
        if(oriForm.getStatus().equals("1")){
            return false;
        }
        oriForm.setName(dynamicForm.getName());
        oriForm.setDescription(dynamicForm.getDescription());
        oriForm.setUpdateTime(LocalDateTime.now());
        oriForm.setVersion(String.valueOf(Integer.parseInt(oriForm.getVersion()) + 1));


        int update = dynamicFormMapper.updateById(oriForm);

        if(!CollectionUtils.isEmpty(dynamicForm.getFormFields())){
            // 判断是否重复fieldId
            List<String> list = dynamicForm.getFormFields().stream().map(DynamicFormField::getFieldId).toList();
            if(list.size() != dynamicForm.getFormFields().size()){
                return false;
            }


            dynamicForm.getFormFields().forEach(formField -> {
                formField.setId(null);
                formField.setDeleted("0");
                formField.setFormId(String.valueOf(oriForm.getId()));
                formField.setVersion(oriForm.getVersion());
                formField.setCreateBy(oriForm.getCreateBy());
                formField.setCreateTime(oriForm.getUpdateTime());
                formField.setUpdateTime(oriForm.getUpdateTime());
            });
            dynamicFormFieldMapper.insert(dynamicForm.getFormFields());
        }


        return update == 1;
    }

    @Override
    public DynamicForm info(DynamicForm dynamicForm){
        List<DynamicFormField> dynamicFormFields = new ArrayList<>();
        if(StringUtils.hasText(dynamicForm.getVersion())){
            dynamicFormFields = dynamicFormFieldMapper.selectList(new LambdaQueryWrapper<DynamicFormField>().eq(DynamicFormField::getFormId, dynamicForm.getId()).eq(DynamicFormField::getVersion, dynamicForm.getVersion()));
            dynamicForm = dynamicFormMapper.selectById(dynamicForm.getId());
        }else {
            dynamicForm = dynamicFormMapper.selectById(dynamicForm.getId());
            dynamicFormFields = dynamicFormFieldMapper.selectList(new LambdaQueryWrapper<DynamicFormField>().eq(DynamicFormField::getFormId, dynamicForm.getId()).eq(DynamicFormField::getVersion, dynamicForm.getVersion()));
        }
        dynamicForm.setFormFields(dynamicFormFields);

        return dynamicForm;
    }

    @Override
    public Page<DynamicForm> queryPage(PageParam pageParam){
        Page<DynamicForm> page = new Page<>(pageParam);
        page = dynamicFormMapper.selectPage(page);
        return page;
    }

    @Override
    public boolean deploy(Integer formId) {
        DynamicForm dynamicForm = dynamicFormMapper.selectById(formId);
        if(dynamicForm.getStatus().equals("1")){
            return false;
        }
        dynamicForm.setUpdateTime(LocalDateTime.now());
        dynamicForm.setStatus("1");
        return dynamicFormMapper.updateById(dynamicForm) == 1;
    }

    @Override
    public boolean stop(Integer formId) {
        DynamicForm dynamicForm = dynamicFormMapper.selectById(formId);
        if(!dynamicForm.getStatus().equals("1")){
            return false;
        }
        dynamicForm.setStatus("-1");
        dynamicForm.setUpdateTime(LocalDateTime.now());
        return dynamicFormMapper.updateById(dynamicForm) == 1;
    }

    @Override
    @Transactional
    public boolean submit(FormData formData) {

        // 获取表单模板信息
        DynamicForm dynamicForm = dynamicFormMapper.selectById(formData.getFormId());
        // 获取表单模板表单项信息
        List<DynamicFormField> dynamicFormFields = dynamicFormFieldMapper.selectList(new LambdaQueryWrapper<DynamicFormField>()
                .eq(DynamicFormField::getFormId, dynamicForm.getId())
                .eq(DynamicFormField::getVersion, formData.getVersion()));

        String userId = Objects.requireNonNull(SecurityUtils.getLoginUser()).getUserId();

        // 获取实例信息
        DynamicFormInstance dynamicFormInstance;
        dynamicFormInstance = dynamicFormInstanceMapper.selectOne(new LambdaQueryWrapper<DynamicFormInstance>().eq(DynamicFormInstance::getId, formData.getFormInstanceId()));
        if(Objects.isNull(dynamicFormInstance)){
            dynamicFormInstance = new DynamicFormInstance()
                    .setFormId(String.valueOf(dynamicForm.getId()))
                    .setVersion(formData.getVersion())
                    .setCreateBy(userId).setDeleted("0").setCreateTime(LocalDateTime.now());
            dynamicFormInstance.setUpdateTime(dynamicFormInstance.getCreateTime());
            dynamicFormInstanceMapper.insert(dynamicFormInstance);
        }



        List<DynamicFormFieldInstance> dynamicFormFieldInstances = dynamicFormFieldInstanceMapper.selectList(new LambdaQueryWrapper<DynamicFormFieldInstance>()
                .eq(DynamicFormFieldInstance::getFormInstanceId, dynamicFormInstance.getId()));
        Map<String, List<DynamicFormFieldInstance>> hasMap =
                dynamicFormFieldInstances.stream().collect(Collectors.toMap(DynamicFormFieldInstance::getFormFieldId, dynamicFormFieldInstance -> dynamicFormFieldInstances));

        DynamicFormInstance finalDynamicFormInstance = dynamicFormInstance;
        dynamicFormFields.forEach(f->{
            List<DynamicFormFieldInstance> dynamicFormFieldInstancesByFieldId = hasMap.get(f.getFieldId());
            DynamicFormFieldInstance dynamicFormFieldInstance;
            if(CollectionUtils.isEmpty(dynamicFormFieldInstancesByFieldId)){
                dynamicFormFieldInstance = new DynamicFormFieldInstance();
                dynamicFormFieldInstance.setVersion(finalDynamicFormInstance.getVersion());
                dynamicFormFieldInstance.setCreateBy(userId);
                dynamicFormFieldInstance.setCreateTime(LocalDateTime.now());
                dynamicFormFieldInstance.setUpdateTime(dynamicFormFieldInstance.getCreateTime());
            }else {
                dynamicFormFieldInstance = dynamicFormFieldInstancesByFieldId.get(0);
                dynamicFormFieldInstance.setUpdateTime(LocalDateTime.now());
            }
            dynamicFormFieldInstance.setDeleted("0");
            dynamicFormFieldInstance.setFormFieldId(f.getId());
            dynamicFormFieldInstance.setFormInstanceId(String.valueOf(finalDynamicFormInstance.getId()));
            dynamicFormFieldInstance.setVal(formData.getData().get(f.getFieldId()));

            dynamicFormFieldInstances.add(dynamicFormFieldInstance);
        });


        dynamicFormFieldInstanceMapper.insertOrUpdate(dynamicFormFieldInstances);


        return true;
    }
}