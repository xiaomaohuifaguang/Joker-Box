package com.cat.simple.mapper;

import com.cat.common.entity.dynamicForm.DynamicFormFieldInstance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import com.cat.common.entity.Page;
import org.apache.ibatis.annotations.Param;


/**
 * <p>
 * 表单项实例id Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-05-06
 */
@Mapper
public interface DynamicFormFieldInstanceMapper extends BaseMapper<DynamicFormFieldInstance> {
   Page<DynamicFormFieldInstance> selectPage(@Param("page") Page<DynamicFormFieldInstance> page);
}
