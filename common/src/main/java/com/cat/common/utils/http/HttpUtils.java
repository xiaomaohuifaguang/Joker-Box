package com.cat.common.utils.http;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

/***
 * <TODO description class purpose>
 * @title HttpUtils
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/8/3 0:36
 **/
public class HttpUtils {


    public String post() throws IOException {
        try(CloseableHttpClient build = HttpClientBuilder.create().build()){
            ClassicHttpRequest httpPost = ClassicRequestBuilder.post("http://httpbin.org/get")
                    .build();
            return build.execute(httpPost, classicHttpResponse -> {
                HttpEntity entity = classicHttpResponse.getEntity();
                return EntityUtils.toString(entity);
            });
        }
    }





    /***
     * 跳过ssl 验证
     * @author xiaomaohuifaguang
     * @date 2023/11/24
     **/
    public static SSLContext NotSSLContext(){
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        try {
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }
        return sslContext;
    }





}
