package com.cat.simple.mapper;

import com.cat.common.entity.dynamicForm.DynamicFormLinkageRule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 动态表单字段联动规则 Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-05-06
 */
@Mapper
public interface DynamicFormLinkageRuleMapper extends BaseMapper<DynamicFormLinkageRule> {

   /**
    * 物理删除指定表单、指定版本的联动规则（绕过 @TableLogic）。
    */
   int deletePhysicsByFormIdAndVersion(@Param("formId") String formId, @Param("version") String version);

   /**
    * 将 sourceVersion 的规则数据复制为 targetVersion。
    */
   int copyVersion(@Param("formId") String formId, @Param("sourceVersion") String sourceVersion, @Param("targetVersion") String targetVersion);

}
