package com.cat.common.utils;

import com.cat.common.entity.utils.UrlToMultipartFile;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;


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


    public static String readTextByPath(String path) throws IOException {
        File file = new File(path);
        if(!file.exists()){
            return null;
        }
        return new String(Files.readAllBytes(Paths.get(path)));

    }


    public static MultipartFile downloadUrlToMultipartFile(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        connection.connect();

        // 读取URL内容
        try (InputStream inputStream = connection.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            // 将输出流转换为字节数组
            byte[] bytes = outputStream.toByteArray();

            // 创建MultipartFile对象
            String fileName = urlString.substring(urlString.lastIndexOf('/') + 1);
            String contentType = connection.getContentType();
            return new UrlToMultipartFile(bytes, fileName, contentType);
        }
    }



}
