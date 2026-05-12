package com.cat.simple.mapper;

import com.cat.common.entity.dynamicForm.DynamicFormLinkageNode;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 动态表单字段联动条件节点 Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-05-06
 */
@Mapper
public interface DynamicFormLinkageNodeMapper extends BaseMapper<DynamicFormLinkageNode> {

   /**
    * 物理删除指定表单、指定版本的节点（绕过 @TableLogic）。
    */
   int deletePhysicsByFormIdAndVersion(@Param("formId") String formId, @Param("version") String version);

   /**
    * 物理删除指定规则ID列表下的所有条件节点（绕过 @TableLogic）。
    */
   int deletePhysicsByRuleIds(@Param("ruleIds") List<String> ruleIds);

   /**
    * 根据规则ID列表查询所有条件节点。
    */
   List<DynamicFormLinkageNode> selectByRuleIds(@Param("ruleIds") List<String> ruleIds);

   /**
    * 将 sourceVersion 的节点数据复制为 targetVersion（需配合 rule 复制后执行，rule_id 会变化）。
    * 注意：此方式仅适用于 rule 复制后 id 保持不变的情况；若 rule id 重新生成，需在 Service 层逐条复制。
    */
   int copyVersion(@Param("formId") String formId, @Param("sourceVersion") String sourceVersion, @Param("targetVersion") String targetVersion);

}
