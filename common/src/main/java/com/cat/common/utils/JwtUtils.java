package com.cat.common.utils;

import io.jsonwebtoken.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.*;

/***
 * JwtUtils
 * @title JwtUtils
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/21 23:06
 **/
public class JwtUtils {

    /**
     * 加解密 key
     */
    private static final String JWT_KEY = "ZjQVqcmnsW/L+8gTXF2RY56kuvWeU3/NeCTSXyeJVWqi64YW3prnGsgDD0sDk8hpceF7DC+WX+Nzz7ERm/fQNh1lg7fEqDZ/AZ9dTY/B3FkzluF2FBdYkT3hHhtZpvedOZrX+kKrupbT9cILoyOBvPE0AwmqcxBWGqn6yjqhKYBEuBN20bPlrI+4O1c0SAdEbe0lGmbGh4Sjb79BCfaNnpgmS5XAHLvE53QQW0ax0YDUaBESSDAbKNWjm3t+3fB1";

    /**
     * @param length 长度
     * @return JWT_KEY
     */
    public static String generateJwtKey(int length) {
        String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            // 从 CHARACTERS 中随机选择一个字符
            int index = secureRandom.nextInt(CHARACTERS.length());
            char randomChar = CHARACTERS.charAt(index);

            // 如果最后一个字符是 /，重新选择一个（除了 / 之外的任意字符）
            if (i == length - 1 && randomChar == '/') {
                index = secureRandom.nextInt(CHARACTERS.length() - 1) + 1; // 减 1 以避免再次选择 '/'
                randomChar = CHARACTERS.charAt(index);
            }
            sb.append(randomChar);
        }
        return sb.toString();
    }

    /**
     * 生成密钥
     *
     * @return 密钥
     */
    public static SecretKey secretKey(String jwtKey) {
        byte[] encodeKey = Base64.getDecoder().decode(jwtKey);
        return new SecretKeySpec(encodeKey, 0, encodeKey.length, "HmacSHA256");
    }

    /**
     * 加密
     *
     * @param map     加密body
     * @param seconds 超时时间（秒）
     * @return 加密字符串
     */
    public static String encrypt(Map<String, Object> map, long seconds) {
        JwtBuilder builder = Jwts.builder();
        // 设置加密方式和密码
        builder.signWith(secretKey(JWT_KEY));
        if(seconds > 0){
            builder.header().add("custom-header", UUID.randomUUID().toString());
//        builder.subject("");
            builder.id(UUID.randomUUID().toString());
            
            builder.issuedAt(new Date());
            builder.expiration(new Date(System.currentTimeMillis() + (seconds * 1000)));
        }
        // 通过map传值
        builder.claims().add(map);
        return builder.compact();
    }

    /**
     * 解密
     *
     * @param encodeStr 通过加密得到的String
     * @return 加密前的body
     */
    public static Map<String, Object> decrypt(String encodeStr) {
        try {
            JwtParserBuilder jwtParserBuilder = Jwts.parser();
            Jws<Claims> claimsJws = jwtParserBuilder.verifyWith(secretKey(JWT_KEY)).build().parseSignedClaims(encodeStr);
            Claims body = claimsJws.getPayload();
            return new HashMap<>(body);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

}
