package com.cat.common.entity.rapidDevelopment;


import com.baomidou.mybatisplus.generator.config.GlobalConfig;
import com.baomidou.mybatisplus.generator.config.ITypeConvert;
import com.baomidou.mybatisplus.generator.config.converts.MySqlTypeConvert;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.IColumnType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.util.StringUtils;

import java.util.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "TableInfo", description = "表信息")
public class TableInfo {

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "表注释")
    private String tableComment;

    @Schema(description = "表名大驼峰")
    private String tableNameUp;

    @Schema(description = "表名小驼峰")
    private String tableNameDown;

    @Schema(description = "字段信息")
    private List<FieldInfo> fieldInfos;

    @Schema(description = "主键")
    private FieldInfo primaryKey;

    @Schema(description = "基础包")
    private String basePackage;

    @Schema(description = "模块")
    private String module;

    @Schema(description = "entity包")
    private String entityPackage;

    @Schema(description = "mapper包")
    private String mapperPackage;

    @Schema(description = "mapperXml包")
    private String mapperXmlPackage;

    @Schema(description = "service包")
    private String servicePackage;

    @Schema(description = "impl包")
    private String implPackage;

    @Schema(description = "controller包")
    private String controllerPackage;


    private Set<String> entityImportPackages = new LinkedHashSet<>();


    public void setTableName(String tableName) {
        this.tableName = tableName;
        this.tableNameUp = convertToEntityNameUp(tableName);
        this.tableNameDown = convertToEntityNameDown(tableName);
    }

    public void setTableComment(String tableComment) {
        this.tableComment = StringUtils.hasText(tableComment) ? tableComment : this.tableName;
    }

    /**
     * 将数据库表名转换为大驼峰形式的实体类名称
     * @param tableName 数据库表名
     * @return 实体类名称
     */
    private static String convertToEntityNameUp(String tableName) {
        // 去除前缀 "cat_"
        if (tableName.startsWith("cat_")) {
            tableName = tableName.substring(4);
        }

        // 将下划线分隔转换为大驼峰形式
        StringBuilder entityName = new StringBuilder();
        boolean nextUpperCase = false;
        for (char c : tableName.toCharArray()) {
            if (c == '_') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    entityName.append(Character.toUpperCase(c));
                    nextUpperCase = false;
                } else {
                    entityName.append(Character.toLowerCase(c));
                }
            }
        }

        // 将首字母大写，以符合大驼峰命名规则
        return Character.toUpperCase(entityName.charAt(0)) + entityName.substring(1);
    }

    /**
     * 将数据库表名转换为小驼峰形式的实体类名称
     * @param tableName 数据库表名
     * @return 实体类名称
     */
    private static String convertToEntityNameDown(String tableName) {
        // 去除前缀 "cat_"
        if (tableName.startsWith("cat_")) {
            tableName = tableName.substring(4);
        }

        // 将下划线分隔转换为小驼峰形式
        StringBuilder entityName = new StringBuilder();
        boolean nextUpperCase = false;
        for (char c : tableName.toCharArray()) {
            if (c == '_') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    entityName.append(Character.toUpperCase(c));
                    nextUpperCase = false;
                } else {
                    entityName.append(Character.toLowerCase(c));
                }
            }
        }

        // 保持首字母小写，以符合小驼峰命名规则
        return entityName.toString();
    }



    public void init(){

        GlobalConfig globalConfig = new GlobalConfig.Builder()
                .dateType(DateType.TIME_PACK)   // 可选：日期映射为 LocalDateTime（老版默认是 Date）
                .build();

        // 2. 拿到默认的 MySQL 转换器
        ITypeConvert converter = new MySqlTypeConvert();
        for (FieldInfo fieldInfo : fieldInfos) {
            fieldInfo.setFieldName(convertToEntityNameDown(fieldInfo.getField()));
            IColumnType columnType = converter.processTypeConvert(globalConfig, fieldInfo.getType());
            fieldInfo.setJavaType(columnType.getType());
            if(StringUtils.hasText(columnType.getPkg())){
                this.entityImportPackages.add(columnType.getPkg());
                if(List.of("LocalDateTime","Date").contains(columnType.getType())){
                    this.entityImportPackages.add("com.fasterxml.jackson.annotation.JsonFormat");
                }
            }
            String extra = fieldInfo.getExtra() == null ? "" : fieldInfo.getExtra().toLowerCase();
            fieldInfo.setKeyFlag("PRI".equals(fieldInfo.getKey()));
            fieldInfo.setKeyIdentityFlag(fieldInfo.getKeyFlag() && extra.contains("auto_increment"));
            fieldInfo.setConvert(!fieldInfo.getField().equals(fieldInfo.getFieldName()));
            fieldInfo.setTsType(toTsType(fieldInfo.getJavaType()));
            fieldInfo.setNullable("YES".equalsIgnoreCase(fieldInfo.getNull()));
            if(!StringUtils.hasText(fieldInfo.getComment())){
                fieldInfo.setComment(fieldInfo.getFieldName());
            }
            if(fieldInfo.getKeyFlag() && Objects.isNull(this.primaryKey)){
                this.primaryKey = fieldInfo;
            }
        }

        this.basePackage = "com.cat";
        this.module = this.tableNameDown;

        this.entityPackage = basePackage + ".common.entity." + this.module;
        this.mapperPackage = basePackage + ".simple." + this.module + ".mapper";
        this.mapperXmlPackage = "mapper";
        this.servicePackage = basePackage + ".simple." + this.module + ".service";
        this.implPackage = basePackage + ".simple." + this.module + ".service.impl";
        this.controllerPackage = basePackage + ".simple." + this.module + ".controller";


    }

    private String toTsType(String javaType) {
        return switch (javaType) {
            case "Integer", "Long", "Double", "Float", "BigDecimal" -> "number";
            case "Boolean" -> "boolean";
            default ->
                // String / LocalDateTime / Date / byte[] 等，JSON 传输时都是字符串
                    "string";
        };
    }


}
