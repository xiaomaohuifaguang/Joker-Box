package com.cat.common.utils;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/***
 * <TODO description class purpose>
 * @title MybatisPlusUtils
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/23 3:43
 **/
public class MybatisPlusUtils {

    final static private String url = "jdbc:mysql://192.168.3.12:3306/joker-box-dev?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8";
    final static private String username = "root";
    final static private String password = "six6";

    /**
     * 代码生成器 请在各服务的AuthMybatisPlusGenerator中引用
     * @param tableName 表名
     * @param outDir 输出路径
     * @param author 作者
     */
    public static void make(String packageName, String tableName, String outDir, String author) {
        FastAutoGenerator.create(url, username, password)
                .globalConfig(builder -> builder
                                .author(author)
                                .outputDir(outDir)
                                .disableOpenDir() // 禁止打开输出目录
                                .commentDate("yyyy-MM-dd")
                                .enableSpringdoc()
//                        .enableSwagger()
                )
                .packageConfig(builder -> builder
                        .parent(packageName)
                                .controller("controller")
                                .entity("entity")
                                .mapper("mapper")
                                .service("service")
                                .serviceImpl("service.impl")
                                .xml("mapper.xml")
                )
                .strategyConfig(builder -> builder
                        .addInclude(tableName)
                        .addTablePrefix("cat_") // 表名前缀
                        .controllerBuilder().disable()
                        .serviceBuilder().disableService().disableServiceImpl()
                        .mapperBuilder().mapperTemplate("/mybatis-plus/freemarker/mapper.java").mapperXmlTemplate("/mybatis-plus/freemarker/mapper.xml").enableFileOverride()
                        .entityBuilder().javaTemplate("/mybatis-plus/freemarker/entity.java").enableFileOverride().enableLombok()
                )
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }

    public static void main(String[] args) {
        String packageName = "com.cat.auth";
        String tableName = "cat_api_path";
        String outDir = "D:\\todo\\mybatis-plus\\joker-box";
        String author = "xiaomaohuifaguang";

        MybatisPlusUtils.make(packageName, tableName,outDir,author);
    }

}
