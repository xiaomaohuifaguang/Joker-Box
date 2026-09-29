package com.cat.simple.form.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.dynamicForm.DynamicFormFieldTemplate;
import org.apache.ibatis.annotations.Mapper;

import com.cat.common.entity.Page;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DynamicFormFieldTemplateMapper extends BaseMapper<DynamicFormFieldTemplate> {
    Page<DynamicFormFieldTemplate> selectPage(@Param("page") Page<DynamicFormFieldTemplate> page, @Param("param") PageParam pageParam);
}
