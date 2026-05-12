package com.cat.simple.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.dynamicForm.DynamicFormField;
import com.cat.common.entity.dynamicForm.DynamicFormFieldType;
import com.cat.common.entity.dynamicForm.DynamicFormOption;
import com.cat.simple.mapper.DynamicFormFieldMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DynamicFormTest {

    @Resource
    private DynamicFormFieldMapper dynamicFormFieldMapper;




//    @PostConstruct
    private void test(){
//        dynamicFormFieldMapper.insert(new DynamicFormField()
//                .setTitle("姓名")
//                .setSpan(24)
//                .setType("input")
////                .setOptions(new ArrayList<>())
//                .setCreateBy("1")
//                .setCreateTime(LocalDateTime.now())
//                .setUpdateTime(LocalDateTime.now())
//        );


//        dynamicFormFieldMapper.insert(new DynamicFormField()
//                .setTitle("姓名")
//                .setSpan(24)
//                .setType(DynamicFormFieldType.INPUT)
//                .setOptions(new ArrayList<>(){{
//                    add(new DynamicFormOption("选项一","1"));
//                    add(new DynamicFormOption("选项二","2"));
//                }})
//                .setCreateBy("1")
//                .setCreateTime(LocalDateTime.now())
//                .setUpdateTime(LocalDateTime.now())
//        );


        List<DynamicFormField> dynamicFormFields = dynamicFormFieldMapper.selectList(new LambdaQueryWrapper<>());

        dynamicFormFields.forEach(System.out::println);


    }



}
