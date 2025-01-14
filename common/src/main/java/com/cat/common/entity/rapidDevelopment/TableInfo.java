package com.cat.common.entity.rapidDevelopment;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "TableInfo", description = "表信息")
public class TableInfo {

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "表名大驼峰")
    private String tableNameUp;

    @Schema(description = "表名小驼峰")
    private String tableNameDown;

    @Schema(description = "字段信息")
    private List<FieldInfo> fieldInfos;

    @Schema(description = "主键")
    private String PrimaryKeyName;

    @Schema(description = "主键小驼峰")
    private String primaryKeyName;


    public TableInfo(String tableName) {
        this.tableName = tableName;
        this.tableNameUp = convertToEntityNameUp(tableName);
        this.tableNameDown = convertToEntityNameDown(tableName);
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
        fieldInfos.forEach(f->{
            f.setFieldName(convertToEntityNameDown(f.getField()));
        });
    }


}
