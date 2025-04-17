package com.cat.simple.mapper;

import com.cat.common.entity.PageParam;
import com.cat.common.entity.mail.MailInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import com.cat.common.entity.Page;
import org.apache.ibatis.annotations.Param;


/**
 * <p>
 * 邮件记录 Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-04-17
 */
@Mapper
public interface MailInfoMapper extends BaseMapper<MailInfo> {
   Page<MailInfo> selectPage(@Param("page") Page<MailInfo> page, @Param("param") PageParam pageParam);
}
