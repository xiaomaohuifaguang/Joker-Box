package com.cat.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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




}
