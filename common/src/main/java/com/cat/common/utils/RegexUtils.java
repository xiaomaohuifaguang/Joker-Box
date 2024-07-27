package com.cat.common.utils;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/***
 * <TODO description class purpose>
 * @title RegexUtils
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/9 23:49
 **/
public class RegexUtils {

    public final static String ACCOUNT_REGEX = "^[A-Za-z][A-Za-z0-9_]{3,19}$";
    public final static String PASSWORD_REGEX = "[A-Za-z0-9!@#$%^&*(),.?/*-+|\\=<>;:\"]{7,19}$";
    public final static String EMAIL_REGEX = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";


    public static boolean validate(String str, String regex) {
        if(!StringUtils.hasText(str)) return false;
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(str).matches();
    }


}
