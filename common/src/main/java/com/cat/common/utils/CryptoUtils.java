package com.cat.common.utils;

import com.cat.common.utils.crypto.SHA256Utils;

/***
 * 加密/验证工具类
 * @title CryptoUtils
 * @description 统一系统加解密
 * @author xiaomaohuifaguang
 * @create 2024/6/23 2:14
 **/
public class CryptoUtils {


    /**
     * 加密
     * @param str 待加密字符串
     * @return 加密字符串
     */
    public static String encrypt(String str){
        return SHA256Utils.encrypt(str);
    }

    /**
     * 验证
     * @param str 未加密字符串
     * @param encryptStr 已加密字符串
     * @return 是否匹配
     */
    public static boolean verify(String str,String encryptStr){
        return SHA256Utils.isHashesEqual(encrypt(str),encryptStr);
    }


}
