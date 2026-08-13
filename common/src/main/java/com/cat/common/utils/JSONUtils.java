package com.cat.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Collections;
import java.util.List;

/***
 * JSON工具类
 * @title JSONUtils
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/24 0:07
 **/
public class JSONUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 对象转换JSON格式字符串
     * @param o 待转换对象
     * @return JSON格式字符串
     */
    public static String toJSONString(Object o){
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObjectByObject(Object o, Class<T> objectClass){
        String jsonStr = toJSONString(o);
        return parseObject(jsonStr, objectClass);
    }

    /**
     * JSON格式字符串转换对象
     * @param jsonStr json字符串
     * @param objectClass 类
     * @return 传入类型的对象
     */
    public static <T> T parseObject(String jsonStr, Class<T> objectClass){
        try {
            return objectMapper.readValue(jsonStr, objectClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    public static <T> List<T> parseListByObject(Object o, Class<T> clazz) {
        String jsonStr = toJSONString(o);
        return parseList(jsonStr, clazz);
    }

    public static <T> List<T> parseList(String jsonStr, Class<T> clazz) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            // ✅ 关键：用 clazz 显式构造 List<T> 的完整类型信息
            JavaType javaType = mapper.getTypeFactory()
                    .constructCollectionType(List.class, clazz);
            return mapper.readValue(jsonStr, javaType);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }




}
