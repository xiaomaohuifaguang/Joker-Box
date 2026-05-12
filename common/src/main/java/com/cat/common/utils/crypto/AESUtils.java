package com.cat.common.utils.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AESUtils {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    private static byte[] getKeyBytes(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[16];
        System.arraycopy(keyBytes, 0, result, 0, Math.min(keyBytes.length, 16));
        return result;
    }

    private static IvParameterSpec getIv(String key) {
        return new IvParameterSpec(getKeyBytes(key));
    }

    public static String encrypt(String data, String key) {
        if (data == null) {
            return null;
        }
        try {
            SecretKeySpec secretKey = new SecretKeySpec(getKeyBytes(key), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, getIv(key));
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES加密失败", e);
        }
    }

    public static String decrypt(String data, String key) {
        if (data == null) {
            return null;
        }
        try {
            SecretKeySpec secretKey = new SecretKeySpec(getKeyBytes(key), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, getIv(key));
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(data));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return data;
        }
    }
}
