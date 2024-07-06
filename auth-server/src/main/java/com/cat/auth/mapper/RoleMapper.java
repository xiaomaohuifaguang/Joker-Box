package com.cat.auth.mapper;

import com.cat.common.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 角色表 Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2024-06-23
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    List<Role> getRolesByUserId(@Param("userId") String userId);

}
