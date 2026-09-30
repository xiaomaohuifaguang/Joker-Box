package com.cat.simple.form.service;


import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.dynamicForm.DynamicFormFieldGroupTemplate;

public interface DynamicFormFieldGroupTemplateService {

    boolean add(DynamicFormFieldGroupTemplate dynamicFormFieldGroupTemplate);

    boolean delete(DynamicFormFieldGroupTemplate dynamicFormFieldGroupTemplate);

    boolean update(DynamicFormFieldGroupTemplate dynamicFormFieldGroupTemplate);

    DynamicFormFieldGroupTemplate info(DynamicFormFieldGroupTemplate dynamicFormFieldGroupTemplate);

    Page<DynamicFormFieldGroupTemplate> queryPage(PageParam pageParam);
}