package com.cat.common.utils;

import org.springframework.http.MediaType;

import java.io.*;


/***
 * <TODO description class purpose>
 * @title IOUtils
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/26 23:49
 **/
public class IOUtils {


    public static String contentType(String fileType){
        return switch (fileType.toLowerCase()) {
            case "png" -> MediaType.IMAGE_PNG_VALUE;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG_VALUE;
            default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
        };
    }

    public static String fileType(String fileName){
        if(fileName.lastIndexOf(".") != -1){
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        return null;
    }

    public static void saveStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        BufferedInputStream in = new BufferedInputStream(inputStream);
        BufferedOutputStream out = new BufferedOutputStream(outputStream);
        byte[] bytes = new byte[1024];
        while (in.read(bytes) != -1){
            out.write(bytes);
        }
        inputStream.close();
        outputStream.close();
    }

    public static void save(File inFile, OutputStream outputStream) throws IOException {
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(inFile));
        BufferedOutputStream out = new BufferedOutputStream(outputStream);
        byte[] bytes = new byte[1024];
        while (in.read(bytes) != -1){
            out.write(bytes);
        }
        in.close();
        out.close();
    }

}
