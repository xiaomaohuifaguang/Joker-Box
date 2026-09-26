package ${mapperPackage};

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import ${entityPackage}.${tableNameUp};
import org.apache.ibatis.annotations.Mapper;

import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ${tableNameUp}Mapper extends BaseMapper<${tableNameUp}> {
    Page<${tableNameUp}> selectPage(@Param("page") Page<${tableNameUp}> page);
}
