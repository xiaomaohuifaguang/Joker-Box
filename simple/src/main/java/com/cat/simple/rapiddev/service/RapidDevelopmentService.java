package com.cat.simple.rapiddev.service;

import com.cat.common.entity.rapidDevelopment.SampleCode;
import freemarker.template.TemplateException;

import java.io.IOException;

public interface RapidDevelopmentService {

    SampleCode generate(String tableName) throws IOException, TemplateException;


}
