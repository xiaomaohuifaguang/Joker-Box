package com.cat.simple.form.service;


import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.dynamicForm.DynamicForm;
import com.cat.common.entity.dynamicForm.FormData;
import com.cat.common.entity.dynamicForm.DynamicFormPublishedVersion;

import java.util.List;

public interface DynamicFormService {

    boolean add(DynamicForm dynamicForm);

    boolean delete(DynamicForm dynamicForm);

    boolean update(DynamicForm dynamicForm);

    DynamicForm info(DynamicForm dynamicForm);

    Page<DynamicForm> queryPage(PageParam pageParam);

    boolean deploy(String formId);

    boolean stop(String formId);

    boolean submit(FormData formData);

    /**
     * 保存表单实例数据（弱校验，适用于流程层）。
     * 不校验表单发布状态，只做字段存在性和必填校验（可跳过）。
     *
     * @param formData     表单数据
     * @param skipRequired 是否跳过必填校验
     * @return 是否成功
     */
    boolean saveFormData(FormData formData, boolean skipRequired);

    List<DynamicFormPublishedVersion> publishedForms();

}