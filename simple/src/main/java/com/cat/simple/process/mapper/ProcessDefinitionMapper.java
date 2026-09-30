package com.cat.simple.process.mapper;

import com.cat.common.entity.process.ProcessDefinition;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.process.ProcessDefinitionPageParam;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import com.cat.common.entity.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;


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
   Page<ProcessDefinition> selectPage(@Param("page") Page<ProcessDefinition> page,@Param("param") ProcessDefinitionPageParam pageParam);







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

   @Update("TRUNCATE TABLE cat_process_instance_form")
   void truncateInstanceForm();

   @Update("TRUNCATE TABLE cat_process_node_field_permission")
   void truncateNodeFieldPermission();

   @Update("TRUNCATE TABLE cat_process_handle_info")
   void truncateHandleInfo();

   @Update("TRUNCATE TABLE cat_process_gateway_condition_node")
   void truncateGatewayConditionNode();

   @Update("TRUNCATE TABLE cat_process_gateway_condition")
   void truncateGatewayCondition();

   @Update("TRUNCATE TABLE cat_process_definition_form")
   void truncateDefinitionForm();

   @Update("TRUNCATE TABLE cat_process_definition_bytearray")
   void truncateDefinitionBytearray();

   @Update("TRUNCATE TABLE cat_process_definition")
   void truncateDefinition();

   @Update("TRUNCATE TABLE cat_process_instance")
   void truncateInstance();





}
