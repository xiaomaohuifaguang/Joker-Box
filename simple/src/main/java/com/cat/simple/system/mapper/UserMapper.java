package com.cat.simple.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.Page;
import com.cat.common.entity.auth.User;
import com.cat.common.entity.auth.UserPageParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 用户表 Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2024-06-23
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    Page<User> selectPage(@Param("page") Page<User> page, @Param("param") UserPageParam param);

    int insertUserAndRole(@Param("userId") String userId, @Param("roleId") String roleId, @Param("createTime") LocalDateTime createTime);

    int removeUserAndRole(@Param("userId") String userId, @Param("roleId") String roleId);

    int userCountByRole(@Param("roleId") String roleId);

    List<User> selectListByIds(@Param("userIds") List<String> userIds);

    List<User> selectListByRole(@Param("roleId") Integer roleId);

    List<User> selectListByRoles(@Param("roleIds") List<String> roleIds);

    List<User> selectListByOrg(@Param("orgId") Integer orgId);

    List<User> selectListByOrgs(@Param("orgIds") List<String> orgIds);


    int insertUserAndOrg(@Param("userId") String userId, @Param("orgId") String orgId, @Param("createTime") LocalDateTime createTime);

    int removeUserAndOrg(@Param("userId") String userId, @Param("orgId") String orgId);


}
