package com.cat.simple.system.mapper;

import com.cat.common.entity.auth.Org;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.auth.OrgPageParam;
import org.apache.ibatis.annotations.Mapper;

import com.cat.common.entity.Page;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * <p>
 * 组织机构表 Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2024-11-29
 */
@Mapper
public interface OrgMapper extends BaseMapper<Org> {
    Page<Org> selectPage(@Param("page") Page<Org> page, @Param("param")OrgPageParam param);


    List<Org> getOrgsByUserId(@Param("userId") String userId);

    List<Integer> getOrgIdsByUserId(@Param("userId") String userId);



}