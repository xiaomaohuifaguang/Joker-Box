package com.cat.simple.config.opensearch.index;

import com.cat.common.entity.ai.chat.QAMessage;
import com.cat.simple.config.opensearch.IndexConfigurable;
import com.cat.simple.config.opensearch.OpensearchUtils;
import jakarta.annotation.Resource;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class QAIndexConfig implements IndexConfigurable {

    @Resource
    private OpensearchUtils opensearchUtils;

    @Override
    public CreateIndexRequest createIndexRequest() {
        return new CreateIndexRequest.Builder()
                .index(opensearchUtils.makeIndexName(QAMessage.INDEX))
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
                                p -> p.keyword(i -> i)
                        )

                        .properties("sessionId",
                                p -> p.keyword(i -> i)
                        )

                        .properties("questionMessageId",
                                p -> p.keyword(i -> i)
                        )

                        .properties("question",
                                p -> p.text(t -> t
                                        .analyzer("ik_max_word")
                                        .searchAnalyzer("ik_smart")
                                        .fields("question_keyword",
                                                f -> f.keyword(k -> k.ignoreAbove(256))
                                        )
                                )
                        )

                        .properties("questionEmbeddings",
                                p -> p.knnVector(k ->
                                        k.dimension(1024).spaceType("l2").mode("on_disk")
                                )
                        )


                        .properties("answerMessageId",
                                p -> p.keyword(i -> i)
                        )

                        .properties("answer",
                                p -> p.text(t -> t
                                        .analyzer("ik_max_word")
                                        .searchAnalyzer("ik_smart")
                                        .fields("answer_keyword",
                                                f -> f.keyword(k -> k.ignoreAbove(256))
                                        )
                                )
                        )

                        .properties("answerEmbeddings",
                                p -> p.knnVector(k ->
                                        k.dimension(1024).spaceType("l2").mode("on_disk")
                                )
                        )

                        .properties("createBy",
                                p -> p.keyword(i -> i)
                        )

                        .properties("createTime",
                                p -> p.date(d -> d
                                        .format("yyyy-MM-dd HH:mm:ss||yyyy-MM-dd'T'HH:mm:ss.SSS||epoch_millis")
                                )
                        )
                )
                .build();
    }
}
