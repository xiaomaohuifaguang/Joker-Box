package com.cat.simple.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.dynamicForm.DynamicFormField;
import org.apache.ibatis.annotations.Mapper;
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

   /**
    * 物理删除指定表单、指定版本的字段定义（绕过 @TableLogic，保留历史版本）。
    */
   int deletePhysicsByFormIdAndVersion(@Param("formId") String formId, @Param("version") String version);

   /**
    * 将 sourceVersion 的数据复制为 targetVersion（用于发布时 DRAFT→新版本，或停用时最新版→DRAFT）。
    */
   int copyVersion(@Param("formId") String formId, @Param("sourceVersion") String sourceVersion, @Param("targetVersion") String targetVersion);
}
