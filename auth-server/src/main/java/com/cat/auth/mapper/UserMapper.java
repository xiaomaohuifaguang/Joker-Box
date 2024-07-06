package com.cat.auth.mapper;

import com.cat.common.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
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

}
