package com.cat.simple.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.common.entity.file.FileInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 文件信息 Mapper 接口
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2024-06-26
 */
@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {

}
