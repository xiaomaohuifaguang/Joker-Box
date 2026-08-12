package com.cat.simple.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.ai.model.AiModel;
import com.cat.common.entity.ai.model.AiModelPageParam;
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
 * @since 2025-12-20
 */
@Mapper
public interface AiModelMapper extends BaseMapper<AiModel> {
   Page<AiModel> selectPage(@Param("page") Page<AiModel> page, @Param("param") AiModelPageParam param);

   AiModel selectDefaultByType(@Param("type") String type);

   int insertOrUpdateDefault(@Param("type") String type, @Param("modelId") String modelId);

   int selectDefaultCountByModelId(@Param("modelId") String modelId);

   int deleteDefaultByType(@Param("type") String type);



}
