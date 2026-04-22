package com.cat.simple.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.dynamicForm.DynamicFormField;
import org.apache.ibatis.annotations.Mapper;

import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import org.apache.ibatis.annotations.Param;


/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-04-30
 */
@Mapper
public interface DynamicFormFieldMapper extends BaseMapper<DynamicFormField> {
   Page<DynamicFormField> selectPage(@Param("page") Page<DynamicFormField> page);
}
