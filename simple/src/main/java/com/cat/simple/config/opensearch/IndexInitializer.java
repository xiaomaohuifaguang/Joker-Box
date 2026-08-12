package com.cat.simple.config.opensearch;

import com.cat.simple.config.opensearch.index.GanDaShiIndexConfig;
import jakarta.annotation.Resource;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IndexInitializer implements ApplicationRunner {

    @Resource
    private OpenSearchClient openSearchClient;

    @Resource
    private OpensearchUtils opensearchUtils;

    @Resource
    private List<IndexConfigurable> indexConfigs;


    @Override
    public void run(ApplicationArguments args) throws Exception {
        for (IndexConfigurable config : indexConfigs) {
            CreateIndexRequest request = config.createIndexRequest();
            String index = request.index();
            try {
                // 1. 检查索引是否已存在
                boolean exists = openSearchClient.indices().exists(e -> {

                    return e.index(index);
                }).value();
                if (!exists) {
                    // 2. 不存在则创建
                    openSearchClient.indices().create(request);
                    System.out.println("✅ 索引 [" + index + "] 创建成功！");
                } else {
                    System.out.println("⏭️ 索引 [" + index + "] 已存在，跳过创建。");
                }
            } catch (Exception e) {
                System.err.println("❌ 索引 [" + index + "] 初始化失败：" + e.getMessage());
            }
        }
    }
}
