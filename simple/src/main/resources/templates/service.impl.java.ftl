package com.cat.simple.service.impl;

import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.${tableNameUp};
import com.cat.simple.mapper.${tableNameUp}Mapper;
import com.cat.simple.service.${tableNameUp}Service;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class ${tableNameUp}ServiceImpl implements ${tableNameUp}Service {


    @Resource
    private ${tableNameUp}Mapper ${tableNameDown}Mapper;

    @Override
    public boolean add(${tableNameUp} ${tableNameDown}){
        return ${tableNameDown}Mapper.insert(${tableNameDown}) == 1;
    }

    @Override
    public boolean delete(${tableNameUp} ${tableNameDown}){
            return ${tableNameDown}Mapper.deleteById(${tableNameDown}) == 1;
    }

    @Override
    public boolean update(${tableNameUp} ${tableNameDown}){
        return ${tableNameDown}Mapper.updateById(${tableNameDown}) == 1;
    }

    @Override
    public ${tableNameUp} info(${tableNameUp} ${tableNameDown}){
        return  ${tableNameDown}Mapper.selectById(${tableNameDown}.getId());
    }

    @Override
    public Page<${tableNameUp}> queryPage(PageParam pageParam){
        Page<${tableNameUp}> page = new Page<>(pageParam);
        page = ${tableNameDown}Mapper.selectPage(page);
        return page;
    }
}