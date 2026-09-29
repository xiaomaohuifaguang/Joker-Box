package com.cat.simple.form.service;


import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.dynamicForm.DynamicFormFieldTemplate;

public interface DynamicFormFieldTemplateService {

    boolean add(DynamicFormFieldTemplate dynamicFormFieldTemplate);

    boolean delete(DynamicFormFieldTemplate dynamicFormFieldTemplate);

    boolean update(DynamicFormFieldTemplate dynamicFormFieldTemplate);

    DynamicFormFieldTemplate info(DynamicFormFieldTemplate dynamicFormFieldTemplate);

    Page<DynamicFormFieldTemplate> queryPage(PageParam pageParam);
}