package com.cat.common.utils.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.io.FileOutputStream;
import java.io.IOException;

public class KeyPairGeneratorUtil {

    public static void main(String[] args) throws Exception {
        // 1. 生成 RSA 密钥对
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048); // 设置密钥长度为 2048 位
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        
        // 2. 获取私钥和公钥
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        
        // 3. 输出私钥
        String privateKeyStr = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        System.out.println("Private Key: \n" + privateKeyStr);

        // 4. 输出公钥
        String publicKeyStr = Base64.getEncoder().encodeToString(publicKey.getEncoded());

        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN PUBLIC KEY-----\n");
        sb.append(publicKeyStr);
        sb.append("\n-----END PUBLIC KEY-----\n");
        System.out.println("Public Key: \n" + sb);
    }

}
