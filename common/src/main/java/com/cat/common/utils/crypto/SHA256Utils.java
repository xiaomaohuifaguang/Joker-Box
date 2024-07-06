package com.cat.common.utils.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/***
 *  SHA256加密
 * @title SHA256Utils
 * @description 散列算法 不可逆
 * @author xiaomaohuifaguang
 * @create 2024/6/23 2:11
 **/
public class SHA256Utils {

    private static final String SALT = "516ed4e4c54da9069088d83fd35f34d3"; // 盐值 可通过generateSalt 自定义


    /**
     * 生成随机盐值
     *
     * @param length 盐的长度
     * @return 随机盐值的十六进制字符串
     */
    public static String generateSalt(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytesToHex(bytes);
    }


    /**
     * 对字符串进行SHA-256散列处理
     *
     * @param input 待散列的字符串
     * @return 散列后的十六进制字符串
     */
    public static String encrypt(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(SALT.getBytes(StandardCharsets.UTF_8)); // 将盐值添加到MessageDigest
            byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param bytes 要转换的字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte aByte : bytes) {
            String hex = Integer.toHexString(0xff & aByte);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 比较两个散列值是否相等
     *
     * @param hash1 第一个散列值
     * @param hash2 第二个散列值
     * @return 如果两个散列值相等返回true，否则返回false
     */
    public static boolean isHashesEqual(String hash1, String hash2) {
        return hash1 != null && hash1.equalsIgnoreCase(hash2);
    }

}
