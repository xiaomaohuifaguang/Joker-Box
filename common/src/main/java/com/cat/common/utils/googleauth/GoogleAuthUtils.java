package com.cat.common.utils.googleauth;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class GoogleAuthUtils {

    // 生成一个密钥
    public static String generateSecret() {
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();  // 返回密钥
    }

    // 根据密钥生成二维码图片
    public static BufferedImage generateQRCodeImage(String secret, String appName, String companyName) throws WriterException {
        String format = "otpauth://totp/"+appName+"?secret=" + secret + "&issuer="+companyName;

        // 配置二维码参数
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 1);  // 边距
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");  // 编码

        // 使用 QRCodeWriter 来生成二维码
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        return toBufferedImage(qrCodeWriter.encode(format, BarcodeFormat.QR_CODE, 300, 300, hints));
    }

    // 将二维码生成的 Matrix 转换成 BufferedImage
    private static BufferedImage toBufferedImage(com.google.zxing.common.BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.createGraphics();

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                image.setRGB(i, j, matrix.get(i, j) ? 0x000000 : 0xFFFFFF);  // 黑白
            }
        }
        return image;
    }


    // 验证用户输入的验证码
    public static boolean verifyCode(String secret, int code) {
        return new GoogleAuthenticator().authorize(secret, code);  // 使用 secret 和用户输入的验证码进行验证
    }



}
