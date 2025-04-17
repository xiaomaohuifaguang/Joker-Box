package com.cat.common.entity.utils;

import org.springframework.web.multipart.MultipartFile;
import java.io.*;

@SuppressWarnings("all")
public class UrlToMultipartFile implements MultipartFile {

    private final byte[] content;
    private final String originalFilename;
    private final String contentType;

    public UrlToMultipartFile(byte[] content, String originalFilename, String contentType) {
        this.content = content;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
    }

    @Override
    public String getName() {
        return "file";
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return content;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        try (OutputStream out = new FileOutputStream(dest)) {
            out.write(content);
        }
    }
}
