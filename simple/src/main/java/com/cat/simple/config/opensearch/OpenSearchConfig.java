package com.cat.simple.config.opensearch;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.reactor.ssl.TlsDetails;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

@Configuration
public class OpenSearchConfig {

    @Value("${custom.opensearch.host}")
    private String OPENSEARCH_HOST;

    @Value("${custom.opensearch.port}")
    private Integer OPENSEARCH_PORT;

    @Value("${custom.opensearch.username}")
    private String OPENSEARCH_USERNAME;

    @Value("${custom.opensearch.password}")
    private String OPENSEARCH_PASSWORD;


    @Bean
    public OpenSearchClient openSearchClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // 1. 创建信任所有证书的 SSLContext（仅限测试环境）
        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(null, (chains, authType) -> true)
                .build();

        HttpHost host = new HttpHost("https", OPENSEARCH_HOST, OPENSEARCH_PORT);

        // 2. 配置用户名密码
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                new AuthScope(host),
                new UsernamePasswordCredentials(OPENSEARCH_USERNAME, OPENSEARCH_PASSWORD.toCharArray())
        );

        // 3. 【核心修复点】将 SSLContext 包装为 TlsStrategy
        TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
                .setSslContext(sslContext)
                // 解决 HTTP/2 协议协商问题 (HTTPCLIENT-2219)
//                .setTlsDetailsFactory(sslEngine -> new TlsDetails(sslEngine.getSession(), sslEngine.getApplicationProtocol()))
                .build();

        // 4. 【核心修复点】将 TlsStrategy 注入到连接管理器中
        PoolingAsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
                .setTlsStrategy(tlsStrategy)
                .build();

        ObjectMapper om = new ObjectMapper();
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        om.registerModule(new JavaTimeModule()); // 处理 createTime 的 LocalDateTime

        // 5. 构建 Transport（不再需要调用 setSSLContext）
        OpenSearchTransport transport =
                ApacheHttpClient5TransportBuilder.builder(host)
                        .setMapper(new JacksonJsonpMapper(om))
                        .setHttpClientConfigCallback(httpClientBuilder ->
                                httpClientBuilder
                                        .setDefaultCredentialsProvider(credentialsProvider)
                                        .setConnectionManager(connectionManager)
                        )
                        .build();


        // 6. 创建 OpenSearch 客户端
        return new OpenSearchClient(transport);


    }



}
