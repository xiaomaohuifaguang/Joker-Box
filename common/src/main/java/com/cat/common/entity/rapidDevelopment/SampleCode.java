package com.cat.common.entity.rapidDevelopment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "SampleCode", description = "样例代码")
public class SampleCode implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主页面.vue")
    private String index;

    @Schema(description = "详情.vue")
    private String info;

    @Schema(description = "新建.vue")
    private String add;

    @Schema(description = "实体.java")
    private String entity;

    @Schema(description = "控制层.java")
    private String controller;

    @Schema(description = "业务层.java")
    private String service;

    @Schema(description = "业务层实现类.java")
    private String impl;

    @Schema(description = "mapper接口.java")
    private String mapper;

    @Schema(description = "mapper实现.java")
    private String xml;

}
