package com.cat.common.utils.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 基于 HttpClient 5 的 HTTP 工具类（包含流式响应）
 */
public class HttpClientUtils {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtils.class);

    private static final ContentType JSON_CONTENT_TYPE = ContentType.APPLICATION_JSON;
    private static final ContentType FORM_CONTENT_TYPE = ContentType.APPLICATION_FORM_URLENCODED;
    private static final long DEFAULT_TIMEOUT = 30;

    private static volatile HttpClientUtils instance;
    private CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 私有构造方法
    private HttpClientUtils() {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .setConnectionRequestTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .setResponseTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .build();

        httpClient = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build();
    }

    /**
     * 获取单例实例
     */
    public static HttpClientUtils getInstance() {
        if (instance == null) {
            synchronized (HttpClientUtils.class) {
                if (instance == null) {
                    instance = new HttpClientUtils();
                }
            }
        }
        return instance;
    }

    /**
     * 同步 GET 请求
     */
    public String get(String url) throws IOException, ParseException {
        return get(url, null, null);
    }

    /**
     * 同步 GET 请求（带参数）
     */
    public String get(String url, Map<String, String> params) throws IOException, ParseException {
        return get(url, params, null);
    }

    /**
     * 同步 GET 请求（带参数和请求头）
     */
    public String get(String url, Map<String, String> params, Map<String, String> headers) throws IOException, ParseException {
        HttpGet httpGet = buildGetRequest(url, params, headers);
        return execute(httpGet);
    }

    /**
     * 流式 GET 请求
     */
    public void getStreaming(String url, Consumer<InputStream> streamConsumer) throws IOException {
        getStreaming(url, null, null, streamConsumer);
    }

    /**
     * 流式 GET 请求（带参数）
     */
    public void getStreaming(String url, Map<String, String> params, Consumer<InputStream> streamConsumer) throws IOException {
        getStreaming(url, params, null, streamConsumer);
    }

    /**
     * 流式 GET 请求（带参数和请求头）
     */
    public void getStreaming(String url, Map<String, String> params, Map<String, String> headers,
                             Consumer<InputStream> streamConsumer) throws IOException {
        HttpGet httpGet = buildGetRequest(url, params, headers);
        executeStreaming(httpGet, streamConsumer);
    }

    /**
     * 同步 POST 表单请求
     */
    public String postForm(String url, Map<String, String> params) throws IOException, ParseException {
        return postForm(url, params, null);
    }

    /**
     * 同步 POST 表单请求（带请求头）
     */
    public String postForm(String url, Map<String, String> params, Map<String, String> headers) throws IOException, ParseException {
        HttpPost httpPost = buildPostRequest(url, headers);

        if (params != null && !params.isEmpty()) {
            List<NameValuePair> formParams = new ArrayList<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                formParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            httpPost.setEntity(new UrlEncodedFormEntity(formParams, StandardCharsets.UTF_8));
        }

        return execute(httpPost);
    }

    /**
     * 流式 POST 表单请求
     */
    public void postFormStreaming(String url, Map<String, String> params, Consumer<InputStream> streamConsumer) throws IOException {
        postFormStreaming(url, params, null, streamConsumer);
    }

    /**
     * 流式 POST 表单请求（带请求头）
     */
    public void postFormStreaming(String url, Map<String, String> params, Map<String, String> headers,
                                  Consumer<InputStream> streamConsumer) throws IOException {
        HttpPost httpPost = buildPostRequest(url, headers);

        if (params != null && !params.isEmpty()) {
            List<NameValuePair> formParams = new ArrayList<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                formParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            httpPost.setEntity(new UrlEncodedFormEntity(formParams, StandardCharsets.UTF_8));
        }

        executeStreaming(httpPost, streamConsumer);
    }

    /**
     * 同步 POST JSON 请求（直接传对象）
     */
    public String postJson(String url, Object object) throws IOException, ParseException {
        return postJson(url, object, null);
    }

    /**
     * 同步 POST JSON 请求（带请求头）
     */
    public String postJson(String url, Object object, Map<String, String> headers) throws IOException, ParseException {
        String json = objectMapper.writeValueAsString(object);
        return postJsonString(url, json, headers);
    }

    /**
     * 流式 POST JSON 请求（直接传对象）
     */
    public void postJsonStreaming(String url, Object object, Consumer<InputStream> streamConsumer) throws IOException {
        postJsonStreaming(url, object, null, streamConsumer);
    }

    /**
     * 流式 POST JSON 请求（带请求头）
     */
    public void postJsonStreaming(String url, Object object, Map<String, String> headers,
                                  Consumer<InputStream> streamConsumer) throws IOException {
        String json = objectMapper.writeValueAsString(object);
        postJsonStringStreaming(url, json, headers, streamConsumer);
    }

    /**
     * 同步 POST JSON 请求（原始字符串）
     */
    public String postJsonString(String url, String json) throws IOException, ParseException {
        return postJsonString(url, json, null);
    }

    /**
     * 同步 POST JSON 请求（原始字符串带请求头）
     */
    public String postJsonString(String url, String json, Map<String, String> headers) throws IOException, ParseException {
        HttpPost httpPost = buildPostRequest(url, headers);
        httpPost.setEntity(new StringEntity(json, JSON_CONTENT_TYPE));
        return execute(httpPost);
    }

    /**
     * 流式 POST JSON 请求（原始字符串）
     */
    public void postJsonStringStreaming(String url, String json, Consumer<InputStream> streamConsumer) throws IOException {
        postJsonStringStreaming(url, json, null, streamConsumer);
    }

    /**
     * 流式 POST JSON 请求（原始字符串带请求头）
     */
    public void postJsonStringStreaming(String url, String json, Map<String, String> headers,
                                        Consumer<InputStream> streamConsumer) throws IOException {
        HttpPost httpPost = buildPostRequest(url, headers);
        httpPost.setEntity(new StringEntity(json, JSON_CONTENT_TYPE));
        executeStreaming(httpPost, streamConsumer);
    }

    /**
     * 文件上传
     */
    public String upload(String url, String fileKey, File file, Map<String, String> params) throws IOException, ParseException {
        return upload(url, fileKey, file, params, null);
    }

    /**
     * 文件上传（带请求头）
     */
    public String upload(String url, String fileKey, File file, Map<String, String> params, Map<String, String> headers) throws IOException, ParseException {
        HttpPost httpPost = buildPostRequest(url, headers);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        if (file != null) {
            builder.addBinaryBody(fileKey, file, ContentType.APPLICATION_OCTET_STREAM, file.getName());
        }

        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.addTextBody(entry.getKey(), entry.getValue());
            }
        }

        httpPost.setEntity(builder.build());
        return execute(httpPost);
    }

    /**
     * 流式文件上传
     */
    public void uploadStreaming(String url, String fileKey, File file, Map<String, String> params,
                                Consumer<InputStream> streamConsumer) throws IOException {
        uploadStreaming(url, fileKey, file, params, null, streamConsumer);
    }

    /**
     * 流式文件上传（带请求头）
     */
    public void uploadStreaming(String url, String fileKey, File file, Map<String, String> params,
                                Map<String, String> headers, Consumer<InputStream> streamConsumer) throws IOException {
        HttpPost httpPost = buildPostRequest(url, headers);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        if (file != null) {
            builder.addBinaryBody(fileKey, file, ContentType.APPLICATION_OCTET_STREAM, file.getName());
        }

        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.addTextBody(entry.getKey(), entry.getValue());
            }
        }

        httpPost.setEntity(builder.build());
        executeStreaming(httpPost, streamConsumer);
    }

    /**
     * 构建 GET 请求
     */
    private HttpGet buildGetRequest(String url, Map<String, String> params, Map<String, String> headers) {
        String finalUrl = buildUrlWithParams(url, params);
        HttpGet httpGet = new HttpGet(finalUrl);
        addHeaders(httpGet, headers);
        return httpGet;
    }

    /**
     * 构建 POST 请求
     */
    private HttpPost buildPostRequest(String url, Map<String, String> headers) {
        HttpPost httpPost = new HttpPost(url);
        addHeaders(httpPost, headers);
        return httpPost;
    }

    /**
     * 构建带参数的 URL
     */
    private String buildUrlWithParams(String url, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return url;
        }

        StringBuilder sb = new StringBuilder(url);
        if (!url.contains("?")) {
            sb.append("?");
        } else if (!url.endsWith("&")) {
            sb.append("&");
        }

        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            sb.append(entry.getKey())
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            first = false;
        }

        return sb.toString();
    }

    /**
     * 添加请求头
     */
    private void addHeaders(HttpUriRequestBase request, Map<String, String> headers) {
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                request.addHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * 执行同步请求
     */
    private String execute(HttpUriRequestBase request) throws IOException, ParseException {
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            EntityUtils.consume(entity);
            return result;
        }
    }

    /**
     * 执行流式请求
     */
    private void executeStreaming(HttpUriRequestBase request, Consumer<InputStream> streamConsumer) throws IOException {
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                try (InputStream inputStream = entity.getContent()) {
                    streamConsumer.accept(inputStream);
                }
            }
            EntityUtils.consume(entity);
        }
    }

    /**
     * 设置自定义 HttpClient
     */
    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * 创建并返回一个 HeaderBuilder 实例，用于简化 header 构建
     */
    public HeaderBuilder headerBuilder() {
        return new HeaderBuilder();
    }

    /**
     * Header 构建器
     */
    public static class HeaderBuilder {
        private final Map<String, String> headers = new HashMap<>();

        public HeaderBuilder add(String key, String value) {
            headers.put(key, value);
            return this;
        }

        public HeaderBuilder contentTypeJson() {
            return add("Content-Type", "application/json");
        }

        public HeaderBuilder contentTypeForm() {
            return add("Content-Type", "application/x-www-form-urlencoded");
        }

        public HeaderBuilder authorization(String token) {
            return add("Authorization", "Bearer " + token);
        }

        public Map<String, String> build() {
            return headers;
        }
    }
}