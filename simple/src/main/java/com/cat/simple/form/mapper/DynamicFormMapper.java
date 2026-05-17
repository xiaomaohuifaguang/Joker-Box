package com.cat.simple.form.mapper;

import com.cat.common.entity.dynamicForm.DynamicForm;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import com.cat.common.entity.Page;
import org.apache.ibatis.annotations.Param;


/**
 * <p>
 * 动态表单 Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-05-02
 */
@Mapper
public interface DynamicFormMapper extends BaseMapper<DynamicForm> {
   Page<DynamicForm> selectPage(@Param("page") Page<DynamicForm> page);
}
