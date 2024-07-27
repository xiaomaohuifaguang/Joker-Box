package com.cat.auth.mapper;

import com.cat.common.entity.Page;
import com.cat.common.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.RolePageParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    int delete(@Param("server") String server, @Param("apiPaths") List<String> apiPaths);


    List<Role> getRoleByPath(@Param("server") String server, @Param("apiPath") String apiPath);

    /**
     * 初始用户添加默认角色
     * @param userId 用户id
     * @return 操作数量
     */
    int defaultRole(@Param("userId") Integer userId);

    Page<Role> selectPage(@Param("page") Page<?> page, @Param("param")RolePageParam param);

    int withUser(@Param("roleId") Integer roleId);

    int withApi(@Param("roleId") Integer roleId);

    int deleteWithUserByRoleId(@Param("roleId") Integer roleId);

    int deleteRoleApiRelation(@Param("roleId") Integer roleId);

    int insertRoleApiRelation(@Param("roleId") Integer roleId, @Param("relations") List<Map<String,String>> relations, @Param("updateTime")LocalDateTime updateTime);

}
