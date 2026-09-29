package com.cat.simple.form.service.impl;

import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.dynamicForm.DynamicFormFieldTemplate;
import com.cat.simple.form.mapper.DynamicFormFieldTemplateMapper;
import com.cat.simple.form.service.DynamicFormFieldTemplateService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class DynamicFormFieldTemplateServiceImpl implements DynamicFormFieldTemplateService {


    @Resource
    private DynamicFormFieldTemplateMapper dynamicFormFieldTemplateMapper;

    @Override
    public boolean add(DynamicFormFieldTemplate dynamicFormFieldTemplate){
        return dynamicFormFieldTemplateMapper.insert(dynamicFormFieldTemplate) == 1;
    }

    @Override
    public boolean delete(DynamicFormFieldTemplate dynamicFormFieldTemplate){
            return dynamicFormFieldTemplateMapper.deleteById(dynamicFormFieldTemplate) == 1;
    }

    @Override
    public boolean update(DynamicFormFieldTemplate dynamicFormFieldTemplate){
        return dynamicFormFieldTemplateMapper.updateById(dynamicFormFieldTemplate) == 1;
    }

    @Override
    public DynamicFormFieldTemplate info(DynamicFormFieldTemplate dynamicFormFieldTemplate){
        return  dynamicFormFieldTemplateMapper.selectById(dynamicFormFieldTemplate.getId());
    }

    @Override
    public Page<DynamicFormFieldTemplate> queryPage(PageParam pageParam){
        Page<DynamicFormFieldTemplate> page = new Page<>(pageParam);
        page = dynamicFormFieldTemplateMapper.selectPage(page, pageParam);
        return page;
    }
}