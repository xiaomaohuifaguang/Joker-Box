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

    @Schema(description = "实体.java")
    private String entity;

    @Schema(description = "mapper接口.java")
    private String mapper;

    @Schema(description = "mapper实现.java")
    private String mapperXml;

    @Schema(description = "业务层.java")
    private String service;

    @Schema(description = "业务层实现类.java")
    private String impl;

    @Schema(description = "控制层.java")
    private String controller;


    @Schema(description = "react前端type.ts")
    private String types;

    @Schema(description = "react 分页hook")
    private String usePage;

    @Schema(description = "react前端api.ts")
    private String api;

    @Schema(description = "react前端page.tsx")
    private String page;

    @Schema(description = "react前端page.tsx 新增/修改弹窗")
    private String formDialog;

    @Schema(description = "react前端类型索引")
    private String typeIndex;

    @Schema(description = "readme")
    private String readme;




}
