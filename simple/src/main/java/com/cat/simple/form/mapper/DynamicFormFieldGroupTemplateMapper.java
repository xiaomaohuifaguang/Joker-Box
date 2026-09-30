package com.cat.simple.form.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.dynamicForm.DynamicFormFieldGroupTemplate;
import org.apache.ibatis.annotations.Mapper;

import com.cat.common.entity.Page;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DynamicFormFieldGroupTemplateMapper extends BaseMapper<DynamicFormFieldGroupTemplate> {
    Page<DynamicFormFieldGroupTemplate> selectPage(@Param("page") Page<DynamicFormFieldGroupTemplate> page, @Param("param")PageParam pageParam);
}
