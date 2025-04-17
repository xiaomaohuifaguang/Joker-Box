package com.cat.simple.config.process.core.utils;

import com.cat.simple.config.bean.SpringContextHolder;
import com.cat.simple.config.process.core.enums.BpmUserTaskStrategyEnum;
import com.cat.simple.mapper.UserMapper;

import java.util.ArrayList;
import java.util.List;


public class FindProcessorUtils {



    public static List<String> find(String candidateStrategy, List<String> candidateParams){
        // 指定人员随机
        if(candidateStrategy.equals(BpmUserTaskStrategyEnum.USER.getStrategy())) {
            return candidateParams;
        }
        if(candidateStrategy.equals(BpmUserTaskStrategyEnum.ROLE.getStrategy())) {
            UserMapper userMapper = SpringContextHolder.getBean(UserMapper.class);
            return userMapper.selectListByRoles(candidateParams).stream().map(m -> String.valueOf(m.getId())).toList();
        }

        return new ArrayList<>();
    }












}
