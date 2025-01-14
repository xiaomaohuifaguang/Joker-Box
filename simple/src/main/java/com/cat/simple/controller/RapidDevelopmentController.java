package com.cat.simple.controller;


import com.cat.common.entity.HttpResult;
import com.cat.common.entity.rapidDevelopment.SampleCode;
import com.cat.simple.service.RapidDevelopmentService;
import freemarker.template.TemplateException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/rapidDevelopmentController")
@Tag(name = "快速开发")
public class RapidDevelopmentController {


    @Resource
    private RapidDevelopmentService rapidDevelopmentService;



    @Operation(summary = "代码生成器")
    @RequestMapping(value = "/generate", method = RequestMethod.POST)
    public HttpResult<SampleCode> generate(@RequestParam("tableName") String tableName) throws IOException, TemplateException {
        return HttpResult.back(rapidDevelopmentService.generate(tableName));
    }





}
