package com.cat.simple.config.opensearch.index;

import com.cat.common.entity.ganDaShi.GanDaShiPost;
import com.cat.simple.config.opensearch.IndexConfigurable;
import com.cat.simple.config.opensearch.OpensearchUtils;
import jakarta.annotation.Resource;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GanDaShiIndexConfig implements IndexConfigurable {

    @Resource
    private OpensearchUtils opensearchUtils;

    @Override
    public CreateIndexRequest createIndexRequest() {

        return new CreateIndexRequest.Builder()
                .index(opensearchUtils.makeIndexName(GanDaShiPost.INDEX))

                // 索引配置
                .settings(s -> s
                        .numberOfShards(1)       // 主分片数量
                        .numberOfReplicas(1)     // 副本分片数量
                        .knn(true)               // 开启 KNN 向量检索
                        .customSettings(Map.of(
                                "index.knn.derived_source.enabled", JsonData.of(false)  // 关闭 knn磁盘优化特性 目的：更新元数据
                        ))
                )

                // 字段映射
                .mappings(m -> m

                        // 主键
                        .properties("id",
                                p -> p.integer(i -> i)
                        )

                        // 标题：全文检索 + 精确匹配
                        .properties("title",
                                p -> p.text(t -> t
                                        .analyzer("ik_max_word")
                                        .searchAnalyzer("ik_smart")
                                        .fields("title_keyword",
                                                f -> f.keyword(k -> k.ignoreAbove(256))
                                        )
                                )
                        )

                        // 内容
                        .properties("content",
                                p -> p.text(t -> t
                                        .analyzer("ik_max_word")
                                        .searchAnalyzer("ik_smart")
                                )
                        )

                        // 正文文本
                        .properties("text",
                                p -> p.text(t -> t
                                        .analyzer("ik_max_word")
                                        .searchAnalyzer("ik_smart")
                                )
                        )

                        // 摘要
                        .properties("digest",
                                p -> p.text(t -> t
                                        .analyzer("ik_max_word")
                                        .searchAnalyzer("ik_smart")
                                )
                        )

                        // 删除标识(String)
                        .properties("deleted",
                                p -> p.keyword(k -> k)
                        )

                        // 创建人
                        .properties("createBy",
                                p -> p.keyword(k -> k)
                        )

                        // 创建时间
                        .properties("createTime",
                                p -> p.date(d -> d
                                        .format("yyyy-MM-dd HH:mm:ss||yyyy-MM-dd'T'HH:mm:ss.SSS||epoch_millis")
                                )
                        )

                        // 浏览次数
                        .properties("viewCount",
                                p -> p.integer(i -> i)
                        )

                        // 文本向量 bge-m3 (1024维)
                        .properties("textEmbeddings",
                                p -> p.knnVector(k ->
                                        k.dimension(1024).spaceType("l2").mode("on_disk")
                                )
                        )
                )

                .build();
    }
}