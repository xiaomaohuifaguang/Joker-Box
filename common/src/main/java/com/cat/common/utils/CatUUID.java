package com.cat.common.utils;

import java.util.UUID;

/***
 * <TODO description class purpose>
 * @title CatUUID
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/26 23:47
 **/
public class CatUUID {

    /**
     * 随机id
     * @return 随机id
     */
    public static String randomUUID(){
        return UUID.randomUUID().toString().replaceAll("-","");
    }


}
