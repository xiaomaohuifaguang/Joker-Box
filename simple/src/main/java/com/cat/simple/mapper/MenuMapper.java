package com.cat.simple.mapper;

import com.cat.common.entity.menu.Menu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.menu.MenuPageParam;
import org.apache.ibatis.annotations.Mapper;

import com.cat.common.entity.Page;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;


/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-01-07
 */
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {


   Page<Menu> selectPage(@Param("page") Page<Menu> page, @Param("param")MenuPageParam param);


   List<Menu> queryAllByAuth(@Param("roleIds") List<Integer> roleIds, @Param("idAdmin") boolean idAdmin);


   int deleteMenuApiRelation(@Param("menuId") Integer menuId);


   int insertMenuApiRelation(@Param("menuId") Integer menuId, @Param("relations") List<HashMap<String,String>> relations, @Param("updateTime")LocalDateTime updateTime);
}