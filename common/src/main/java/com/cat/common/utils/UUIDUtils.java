package com.cat.common.utils;

import com.cat.common.utils.datetime.DateTimeUtils;

import java.time.LocalDate;
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

    public static String orderNo(){
        String str="ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        return DateTimeUtils.getFormatStrByLocalDate(LocalDate.now(), DateTimeUtils.DATE_FORMAT_YMD) + getRondomString(8, str);
    }


    private static String getRandomString(int length){
        String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        return getRondomString(length, str);
    }

    private static String getRondomString(int length, String charBase){
        Random random=new Random();
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<length;i++){
            int number=random.nextInt(charBase.length());
            sb.append(charBase.charAt(number));
        }
        return sb.toString();
    }






}
