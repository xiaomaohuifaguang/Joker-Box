package com.cat.common.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
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
 * defaultValue 专用类型处理器。
 * 字符串原样存储（不加 JSON 引号），数组/对象走 JSON 序列化。
 */
@MappedTypes(Object.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class DefaultValueTypeHandler extends BaseTypeHandler<Object> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException {
        if (parameter instanceof String) {
            ps.setString(i, (String) parameter);
        } else {
            ps.setString(i, JSON.toJSONString(parameter));
        }
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

    private Object parse(String value) {

        System.out.println("结构化值"+value);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("[") && !trimmed.startsWith("{")) {
            return value;
        }
        try {
            Object result = JSON.parse(trimmed);
            if (result instanceof JSONObject jo) {
                // 用 getInnerMap() 或 new HashMap<>(jo) 确保返回标准 Map
                return new java.util.HashMap<>(jo);
            }
            if (result instanceof JSONArray ja) {
                // ✅ 使用 toJavaObject 代替 to，行为更确定
                return ja.to(List.class);
                // 或者更保险的做法：
                // return new ArrayList<>(ja);
            }
            return result;
        } catch (Exception e) {
            // ✅ 建议至少打个日志，否则这类 bug 极难排查
            // log.warn("DefaultValueTypeHandler parse failed, raw value: {}", value, e);
            return value;
        }
    }


}