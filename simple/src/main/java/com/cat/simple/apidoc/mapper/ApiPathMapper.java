package com.cat.simple.apidoc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.Page;
import com.cat.common.entity.auth.ApiPath;
import com.cat.common.entity.auth.ApiPathGroup;
import com.cat.common.entity.auth.ApiPathPageParam;
import com.cat.common.entity.auth.ApiPathServer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2024-07-11
 */
@Mapper
public interface ApiPathMapper extends BaseMapper<ApiPath> {

    List<ApiPathServer> servers();
    List<ApiPathGroup> groups(@Param("server") String server);

    List<ApiPath> selectListByRoleId(@Param("roleId") String roleId,@Param("server") String server,@Param("groupName") String groupName);

    List<ApiPath> selectListByMenuId(@Param("menuId") String menuId,@Param("server") String server,@Param("groupName") String groupName);

    Page<ApiPath> selectPage(@Param("page") Page<ApiPath> page, @Param("param") ApiPathPageParam param);

    List<HashMap<String, String>> selectAllByRoleId(@Param("roleId") String roleId);

}
