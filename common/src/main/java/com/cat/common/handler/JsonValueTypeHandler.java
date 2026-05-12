package com.cat.common.handler;

import com.alibaba.fastjson2.JSON;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 自定义 JSON 类型处理器。
 * 解决 Fastjson2TypeHandler 对 String 类型值序列化后多出引号的问题：
 * 例如传入 "aaa"，存入数据库为 JSON 字符串 "aaa"，
 * 但某些版本 fastjson2 读取时会保留外层引号变成 ""aaa""，
 * 本处理器在读取时做兼容性去引号处理。
 */
@MappedTypes(Object.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class JsonValueTypeHandler extends BaseTypeHandler<Object> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, JSON.toJSONString(parameter));
    }

    @Override
    public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private Object parse(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            Object result = JSON.parse(json);
            // fastjson2 的 JSONObject/JSONArray 转为标准 JDK 类型，确保 Jackson 正确序列化
            if (result instanceof com.alibaba.fastjson2.JSONObject jo) {
                return jo.to(Map.class);
            }
            if (result instanceof com.alibaba.fastjson2.JSONArray ja) {
                return ja.to(List.class);
            }
            // 兼容性处理：某些场景下 fastjson2 解析 JSON 字符串后仍保留外层引号
            if (result instanceof String s && s.length() >= 2) {
                String trimmedJson = json.trim();
                if (trimmedJson.startsWith("\"") && trimmedJson.endsWith("\"")
                        && s.startsWith("\"") && s.endsWith("\"")) {
                    return s.substring(1, s.length() - 1);
                }
            }
            return result;
        } catch (Exception e) {
            return json;
        }
    }
}
