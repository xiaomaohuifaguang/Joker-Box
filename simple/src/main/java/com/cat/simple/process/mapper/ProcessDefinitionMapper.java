package com.cat.simple.process.mapper;

import com.cat.common.entity.PageParam;
import com.cat.common.entity.process.ProcessDefinition;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import com.cat.common.entity.Page;
import org.apache.ibatis.annotations.Param;


/**
 * <p>
 * 流程定义信息表 Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-02-27
 */
@Mapper
public interface ProcessDefinitionMapper extends BaseMapper<ProcessDefinition> {
   Page<ProcessDefinition> selectPage(@Param("page") Page<ProcessDefinition> page,@Param("param") PageParam pageParam);







   @Delete("DELETE FROM cat_process_instance_form")
   int deleteInstanceForm();

   @Delete("DELETE FROM cat_process_node_field_permission")
   int deleteNodeFieldPermission();

   @Delete("DELETE FROM cat_process_handle_info")
   int deleteHandleInfo();

   @Delete("DELETE FROM cat_process_gateway_condition_node")
   int deleteGatewayConditionNode();

   @Delete("DELETE FROM cat_process_gateway_condition")
   int deleteGatewayCondition();

   @Delete("DELETE FROM cat_process_definition_form")
   int deleteDefinitionForm();

   @Delete("DELETE FROM cat_process_definition_bytearray")
   int deleteDefinitionBytearray();

   @Delete("DELETE FROM cat_process_definition")
   int deleteDefinition();

   @Delete("DELETE FROM cat_process_instance")
   int deleteInstance();





}
