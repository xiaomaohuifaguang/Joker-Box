package com.cat.simple.form.mapper;

import com.cat.common.entity.dynamicForm.DynamicFormInstance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import com.cat.common.entity.Page;
import org.apache.ibatis.annotations.Param;


/**
 * <p>
 * 动态表单实例 Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-05-06
 */
@Mapper
public interface DynamicFormInstanceMapper extends BaseMapper<DynamicFormInstance> {
   Page<DynamicFormInstance> selectPage(@Param("page") Page<DynamicFormInstance> page);
}
