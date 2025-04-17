package com.cat.common.utils.who;

import java.security.SecureRandom;
import java.util.Random;

public class RandomUserNameUtils {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String CHARACTERS_NUMBERS_UNDERSCORE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_";
    private static final Random random = new SecureRandom();

    public static String make() {
        // 随机选择一个长度，范围是4到20（因为第一个字符是字母，所以长度范围是3+1到19+1）
        int length = 4 + random.nextInt(17);

        // 随机选择一个字母作为第一个字符
        char firstChar = CHARACTERS.charAt(random.nextInt(CHARACTERS.length()));

        // 生成剩余的字符
        StringBuilder sb = new StringBuilder(length);
        sb.append(firstChar);
        for (int i = 1; i < length; i++) {
            sb.append(CHARACTERS_NUMBERS_UNDERSCORE.charAt(random.nextInt(CHARACTERS_NUMBERS_UNDERSCORE.length())));
        }

        return sb.toString();
    }
}
