package com.cat.common.utils.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 优化版 OkHttp 工具类（包含流式请求响应功能）
 */
public class OkHttpUtils {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
    private static final long DEFAULT_TIMEOUT = 30;

    private static volatile OkHttpUtils instance;
    private OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 私有构造方法
    private OkHttpUtils() {
        okHttpClient = OkHttpUtils.getDefaultClient();
    }

    public static OkHttpClient getDefaultClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);

        return builder.build();
    }

    /**
     * 获取单例实例
     */
    public static OkHttpUtils getInstance() {
        if (instance == null) {
            synchronized (OkHttpUtils.class) {
                if (instance == null) {
                    instance = new OkHttpUtils();
                }
            }
        }
        return instance;
    }

    /**
     * 流式请求响应（GET）
     */
    public void getStreaming(String url, Callback callback) {
        getStreaming(url, null, null, callback);
    }

    /**
     * 流式请求响应（GET带参数）
     */
    public void getStreaming(String url, Map<String, String> params, Callback callback) {
        getStreaming(url, params, null, callback);
    }

    /**
     * 流式请求响应（GET带参数和请求头）
     */
    public void getStreaming(String url, Map<String, String> params, Map<String, String> headers, Callback callback) {
        Request request = buildGetRequest(url, params, headers);
        enqueue(request, callback);
    }

    /**
     * 流式请求响应（POST JSON）
     */
    public void postJsonStreaming(String url, Object object, Callback callback) {
        postJsonStreaming(url, object, null, callback);
    }

    /**
     * 流式请求响应（POST JSON带请求头）
     */
    public void postJsonStreaming(String url, Object object, Map<String, String> headers, Callback callback) {
        try {
            String json = objectMapper.writeValueAsString(object);
            RequestBody body = RequestBody.create(json, JSON);
            postStreaming(url, body, headers, callback);
        } catch (IOException e) {
            callback.onFailure(null, e);
        }
    }

    /**
     * 流式请求响应（POST JSON字符串）
     */
    public void postJsonStringStreaming(String url, String json, Callback callback) {
        postJsonStringStreaming(url, json, null, callback);
    }

    /**
     * 流式请求响应（POST JSON字符串带请求头）
     */
    public void postJsonStringStreaming(String url, String json, Map<String, String> headers, Callback callback) {
        RequestBody body = RequestBody.create(json, JSON);
        postStreaming(url, body, headers, callback);
    }

    /**
     * 流式请求响应（POST表单）
     */
    public void postFormStreaming(String url, Map<String, String> params, Callback callback) {
        postFormStreaming(url, params, null, callback);
    }

    /**
     * 流式请求响应（POST表单带请求头）
     */
    public void postFormStreaming(String url, Map<String, String> params, Map<String, String> headers, Callback callback) {
        FormBody.Builder builder = new FormBody.Builder();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.add(entry.getKey(), entry.getValue());
            }
        }
        RequestBody body = builder.build();
        postStreaming(url, body, headers, callback);
    }

    /**
     * 流式请求响应（通用POST）
     */
    public void postStreaming(String url, RequestBody body, Map<String, String> headers, Callback callback) {
        Request request = buildPostRequest(url, body, headers);
        enqueue(request, callback);
    }

    // 以下是原有方法保持不变
    // ... [原有代码保持不变] ...

    /**
     * 同步 GET 请求
     */
    public String get(String url) throws IOException {
        return get(url, null, null);
    }

    /**
     * 同步 GET 请求（带参数）
     */
    public String get(String url, Map<String, String> params) throws IOException {
        return get(url, params, null);
    }

    /**
     * 同步 GET 请求（带参数和请求头）
     */
    public String get(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
        Request request = buildGetRequest(url, params, headers);
        return execute(request);
    }

    /**
     * 异步 GET 请求
     */
    public void getAsync(String url, Callback callback) {
        getAsync(url, null, null, callback);
    }

    /**
     * 异步 GET 请求（带参数）
     */
    public void getAsync(String url, Map<String, String> params, Callback callback) {
        getAsync(url, params, null, callback);
    }

    /**
     * 异步 GET 请求（带参数和请求头）
     */
    public void getAsync(String url, Map<String, String> params, Map<String, String> headers, Callback callback) {
        Request request = buildGetRequest(url, params, headers);
        enqueue(request, callback);
    }

    /**
     * 同步 POST 表单请求
     */
    public String postForm(String url, Map<String, String> params) throws IOException {
        return postForm(url, params, null);
    }

    /**
     * 同步 POST 表单请求（带请求头）
     */
    public String postForm(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
        FormBody.Builder builder = new FormBody.Builder();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.add(entry.getKey(), entry.getValue());
            }
        }
        RequestBody body = builder.build();
        return post(url, body, headers);
    }

    /**
     * 同步 POST JSON 请求（直接传对象）
     */
    public String postJson(String url, Object object) throws IOException {
        return postJson(url, object, null);
    }

    /**
     * 同步 POST JSON 请求（带请求头）
     */
    public String postJson(String url, Object object, Map<String, String> headers) throws IOException {
        String json = objectMapper.writeValueAsString(object);
        RequestBody body = RequestBody.create(json, JSON);
        return post(url, body, headers);
    }

    /**
     * 同步 POST JSON 请求（原始字符串）
     */
    public String postJsonString(String url, String json) throws IOException {
        return postJsonString(url, json, null);
    }

    /**
     * 同步 POST JSON 请求（原始字符串带请求头）
     */
    public String postJsonString(String url, String json, Map<String, String> headers) throws IOException {
        RequestBody body = RequestBody.create(json, JSON);
        return post(url, body, headers);
    }

    /**
     * 同步 POST 请求
     */
    public String post(String url, RequestBody body, Map<String, String> headers) throws IOException {
        Request request = buildPostRequest(url, body, headers);
        return execute(request);
    }

    /**
     * 异步 POST 表单请求
     */
    public void postFormAsync(String url, Map<String, String> params, Callback callback) {
        postFormAsync(url, params, null, callback);
    }

    /**
     * 异步 POST 表单请求（带请求头）
     */
    public void postFormAsync(String url, Map<String, String> params, Map<String, String> headers, Callback callback) {
        FormBody.Builder builder = new FormBody.Builder();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.add(entry.getKey(), entry.getValue());
            }
        }
        RequestBody body = builder.build();
        postAsync(url, body, headers, callback);
    }

    /**
     * 异步 POST JSON 请求（直接传对象）
     */
    public void postJsonAsync(String url, Object object, Callback callback) {
        postJsonAsync(url, object, null, callback);
    }

    /**
     * 异步 POST JSON 请求（带请求头）
     */
    public void postJsonAsync(String url, Object object, Map<String, String> headers, Callback callback) {
        try {
            String json = objectMapper.writeValueAsString(object);
            RequestBody body = RequestBody.create(json, JSON);
            postAsync(url, body, headers, callback);
        } catch (IOException e) {
            callback.onFailure(null, e);
        }
    }

    /**
     * 异步 POST JSON 请求（原始字符串）
     */
    public void postJsonStringAsync(String url, String json, Callback callback) {
        postJsonStringAsync(url, json, null, callback);
    }

    /**
     * 异步 POST JSON 请求（原始字符串带请求头）
     */
    public void postJsonStringAsync(String url, String json, Map<String, String> headers, Callback callback) {
        RequestBody body = RequestBody.create(json, JSON);
        postAsync(url, body, headers, callback);
    }

    /**
     * 异步 POST 请求
     */
    public void postAsync(String url, RequestBody body, Map<String, String> headers, Callback callback) {
        Request request = buildPostRequest(url, body, headers);
        enqueue(request, callback);
    }

    /**
     * 文件上传
     */
    public String upload(String url, String fileKey, File file, Map<String, String> params) throws IOException {
        return upload(url, fileKey, file, params, null);
    }

    /**
     * 文件上传（带请求头）
     */
    public String upload(String url, String fileKey, File file, Map<String, String> params, Map<String, String> headers) throws IOException {
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        // 添加文件
        if (file != null) {
            RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/octet-stream"));
            builder.addFormDataPart(fileKey, file.getName(), fileBody);
        }

        // 添加其他参数
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.addFormDataPart(entry.getKey(), entry.getValue());
            }
        }

        RequestBody body = builder.build();
        Request request = buildPostRequest(url, body, headers);
        return execute(request);
    }

    /**
     * 构建 GET 请求
     */
    private Request buildGetRequest(String url, Map<String, String> params, Map<String, String> headers) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
            }
        }

        Request.Builder builder = new Request.Builder()
                .url(urlBuilder.build())
                .get();

        addHeaders(builder, headers);
        return builder.build();
    }

    /**
     * 构建 POST 请求
     */
    private Request buildPostRequest(String url, RequestBody body, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body);

        addHeaders(builder, headers);
        return builder.build();
    }

    /**
     * 添加请求头
     */
    private void addHeaders(Request.Builder builder, Map<String, String> headers) {
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * 执行同步请求
     */
    private String execute(Request request) throws IOException {
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            ResponseBody body = response.body();
            return body != null ? body.string() : null;
        }
    }

    /**
     * 执行异步请求
     */
    private void enqueue(Request request, Callback callback) {
        okHttpClient.newCall(request).enqueue(callback);
    }

    /**
     * 设置自定义 OkHttpClient
     */
    public void setOkHttpClient(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
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