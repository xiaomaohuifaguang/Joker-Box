package com.cat.simple.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.dynamicForm.DynamicFormFieldGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 动态表单字段分组 Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-05-09
 */
@Mapper
public interface DynamicFormFieldGroupMapper extends BaseMapper<DynamicFormFieldGroup> {

    /**
     * 物理删除指定表单、指定版本的分组（绕过 @TableLogic）。
     */
    int deletePhysicsByFormIdAndVersion(@Param("formId") String formId, @Param("version") String version);
}
