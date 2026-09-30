package com.cat.simple.form.service.impl;

import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.dynamicForm.DynamicFormFieldGroupTemplate;
import com.cat.simple.form.mapper.DynamicFormFieldGroupTemplateMapper;
import com.cat.simple.form.service.DynamicFormFieldGroupTemplateService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class DynamicFormFieldGroupTemplateServiceImpl implements DynamicFormFieldGroupTemplateService {


    @Resource
    private DynamicFormFieldGroupTemplateMapper dynamicFormFieldGroupTemplateMapper;

    @Override
    public boolean add(DynamicFormFieldGroupTemplate dynamicFormFieldGroupTemplate){
        return dynamicFormFieldGroupTemplateMapper.insert(dynamicFormFieldGroupTemplate) == 1;
    }

    @Override
    public boolean delete(DynamicFormFieldGroupTemplate dynamicFormFieldGroupTemplate){
            return dynamicFormFieldGroupTemplateMapper.deleteById(dynamicFormFieldGroupTemplate) == 1;
    }

    @Override
    public boolean update(DynamicFormFieldGroupTemplate dynamicFormFieldGroupTemplate){
        return dynamicFormFieldGroupTemplateMapper.updateById(dynamicFormFieldGroupTemplate) == 1;
    }

    @Override
    public DynamicFormFieldGroupTemplate info(DynamicFormFieldGroupTemplate dynamicFormFieldGroupTemplate){
        return  dynamicFormFieldGroupTemplateMapper.selectById(dynamicFormFieldGroupTemplate.getId());
    }

    @Override
    public Page<DynamicFormFieldGroupTemplate> queryPage(PageParam pageParam){
        Page<DynamicFormFieldGroupTemplate> page = new Page<>(pageParam);
        page = dynamicFormFieldGroupTemplateMapper.selectPage(page, pageParam);
        return page;
    }
}