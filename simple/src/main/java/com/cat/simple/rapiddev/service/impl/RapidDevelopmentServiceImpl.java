package com.cat.simple.rapiddev.service.impl;

import com.cat.common.entity.rapidDevelopment.FieldInfo;
import com.cat.common.entity.rapidDevelopment.SampleCode;
import com.cat.common.entity.rapidDevelopment.TableInfo;
import com.cat.common.utils.IOUtils;
import com.cat.common.utils.MybatisPlusUtils;
import com.cat.simple.rapiddev.mapper.RapidDevelopmentMapper;
import com.cat.simple.rapiddev.service.RapidDevelopmentService;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Service
public class RapidDevelopmentServiceImpl implements RapidDevelopmentService {

    @Resource
    private FreeMarkerConfigurer freeMarkerConfigurer;

    @Resource
    private RapidDevelopmentMapper rapidDevelopmentMapper;


    @Override
    public SampleCode generate(String tableName) throws IOException, TemplateException {

        TableInfo tableInfo = new TableInfo(tableName);

        List<FieldInfo> fieldInfos = rapidDevelopmentMapper.queryAllFields(tableName);
        tableInfo.setFieldInfos(fieldInfos);
        tableInfo.init();

        SampleCode sampleCode = new SampleCode();

        String packageName = "com.cat";
        String outDir = "mybatis-plus-out/joker-box";
        String author = "xiaomaohuifaguang";
        MybatisPlusUtils.make(packageName, tableName,outDir,author);
        String packagePath = packageName.replace(".","/");

        // 实体类java 生成
        String entityPath = outDir + "/"+packagePath+"/common/entity/"+tableInfo.getTableNameUp()+".java";
        String entity = IOUtils.readTextByPath(entityPath);
        sampleCode.setEntity(entity);
        Files.delete(Path.of(entityPath));

        // mapper接口 生成
        String mapperPath = outDir + "/"+packagePath+"/simple/mapper/"+tableInfo.getTableNameUp()+"Mapper.java";
        String mapper = IOUtils.readTextByPath(mapperPath);
        sampleCode.setMapper(mapper);
        Files.delete(Path.of(mapperPath));

        // mapper xml 生成
        String xlmPath = outDir + "/"+packagePath+"/simple/mapper/xml/"+tableInfo.getTableNameUp()+"Mapper.xml";
        String xml = IOUtils.readTextByPath(xlmPath);
        sampleCode.setXml(xml);
        Files.delete(Path.of(xlmPath));

        // 业务层接口 生成
        String service = makeService(tableInfo);
        sampleCode.setService(service);

        // 业务层实现 生成
        String impl = makeServiceImpl(tableInfo);
        sampleCode.setImpl(impl);

        // 控制层 生成
        String controller = makeController(tableInfo);
        sampleCode.setController(controller);

        // vue 列表页
        String indexView = makeIndexView(tableInfo);
        sampleCode.setIndex(indexView);

        // vue 详情/编辑页
        String infoView = makeInfoView(tableInfo);
        sampleCode.setInfo(infoView);

        String addView = makeAddView(tableInfo);
        sampleCode.setAdd(addView);

        return sampleCode;
    }


    private String makeService(TableInfo tableInfo) throws IOException, TemplateException {
        Template template;
        template = freeMarkerConfigurer.getConfiguration().getTemplate("service.java.ftl");
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, tableInfo);
    }

    private String makeServiceImpl(TableInfo tableInfo) throws TemplateException, IOException {
        Template template;
        template = freeMarkerConfigurer.getConfiguration().getTemplate("service.impl.java.ftl");
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, tableInfo);
    }

    private String makeController(TableInfo tableInfo) throws TemplateException, IOException {
        Template template;
        template = freeMarkerConfigurer.getConfiguration().getTemplate("controller.java.ftl");
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, tableInfo);
    }

    private String makeInfoView(TableInfo tableInfo) throws TemplateException, IOException {
        Template template;
        template = freeMarkerConfigurer.getConfiguration().getTemplate("infoView.vue.ftl");
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, tableInfo);
    }

    private String makeIndexView(TableInfo tableInfo) throws TemplateException, IOException {
        Template template;
        template = freeMarkerConfigurer.getConfiguration().getTemplate("indexView.vue.ftl");
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, tableInfo);
    }

    private String makeAddView(TableInfo tableInfo) throws TemplateException, IOException {
        Template template;
        template = freeMarkerConfigurer.getConfiguration().getTemplate("addView.vue.ftl");
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, tableInfo);
    }












}
