package com.cat.common.entity;

import org.springframework.web.multipart.MultipartFile;
import java.io.*;

/**
 * 将本地 File 包装成 Spring MultipartFile
 * 适用于生产环境 Service 层，无需额外依赖
 */
public class LocalMultipartFile implements MultipartFile {

    private final String name;
    private final String originalFilename;
    private final String contentType;
    private final File file;

    public LocalMultipartFile(File file, String name, String contentType) {
        this.file = file;
        this.name = name;
        this.originalFilename = file.getName();
        this.contentType = contentType;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getOriginalFilename() {
        return this.originalFilename;
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    @Override
    public boolean isEmpty() {
        return file.length() == 0;
    }

    @Override
    public long getSize() {
        return file.length();
    }

    @Override
    public byte[] getBytes() throws IOException {
        return java.nio.file.Files.readAllBytes(file.toPath());
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        java.nio.file.Files.copy(file.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
}
