package com.cat.common.utils;

import java.util.Random;
import java.util.UUID;

/***
 * 随机id生成器
 * @title UUIDUtils
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/8 1:16
 **/
public class UUIDUtils {

    public static String randomUUID(){
        return UUID.randomUUID().toString().replaceAll("-","");
    }


    public static String code(int length){
        return getRandomString(length);
    }

    private static String getRandomString(int length){
        String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random=new Random();
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<length;i++){
            int number=random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }
}
