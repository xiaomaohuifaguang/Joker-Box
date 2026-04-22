package com.cat.simple.service;


import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.dynamicForm.DynamicForm;
import com.cat.common.entity.dynamicForm.FormData;

public interface DynamicFormService {

    boolean add(DynamicForm dynamicForm);

    boolean delete(DynamicForm dynamicForm);

    boolean update(DynamicForm dynamicForm);

    DynamicForm info(DynamicForm dynamicForm);

    Page<DynamicForm> queryPage(PageParam pageParam);

    boolean deploy(Integer formId);

    boolean stop(Integer formId);

    boolean submit(FormData formData);

}