package com.cat.simple.config.opensearch;

import com.cat.common.entity.Page;
import jakarta.annotation.Resource;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.SourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * OpenSearch 常用操作工具类。
 * 文档统一用 {@link Map} 承载；如需强类型，可自行扩展泛型方法。
 */
@Component
public class OpensearchUtils {

    @Value("${opensearch.index_prefix}")
    private String INDEX_PREFIX;

    @Resource
    private OpenSearchClient openSearchClient;

    public <T> boolean insertOrUpdate(String indexName, String id, T document) {
        String index = makeIndexName(indexName);
        IndexRequest<T> indexRequest = new IndexRequest.Builder<T>()
                .index(index)
                .id(id)
                .document(document)
                .refresh(Refresh.True) // 写入后立即刷新，确保立即可见
                .build();


        IndexResponse response = null;
        try {
            response = openSearchClient.index(indexRequest);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // 返回 CREATED 或 UPDATED 均视为成功
        return response.result().name().equals("Created") || response.result().name().equals("Updated");
    }

    public <T> boolean update(String indexName, String id, T t, Class<T> classZ) {
        String index = makeIndexName(indexName);
        UpdateRequest<T, T> updateRequest = new UpdateRequest.Builder<T, T>()
                .index(index).id(id).doc(t).build();
        UpdateResponse<T> response = null;
        try {
            response = openSearchClient.update(updateRequest, classZ);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return response.result().name().equals("Updated");
    }



    public <T> Page<T> searchPage( String indexName, Page<T> page, Query query, SourceConfig sourceConfig, Class<T> clazz, SortOptions... sortOptions){
        String index = makeIndexName(indexName);


        SearchRequest searchRequest = SearchRequest.of(s -> {
                    s.index(index)
                            .trackTotalHits(t -> t.enabled(true));

                    if(query == null){
                        s.query(Query.of(q -> q.matchAll(m -> m)));
                    }else {
                        s.query(query);
                    }
                    if (sourceConfig != null) {
                        s.source(sourceConfig);
                    }
                    if(sortOptions != null && sortOptions.length > 0){
                        s.sort(Arrays.asList(sortOptions));
                    }

                    s.from((int) ((page.getCurrent() - 1) * page.getSize()) );
                    s.size((int) page.getSize());

                    return s;
                }
        );

        SearchResponse<T> response = null;
        try {
            response = openSearchClient.search(searchRequest, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        long total = response.hits().total() != null ? response.hits().total().value() : 0;
        // 5. 提取当前页数据列表
        List<T> list = response.hits().hits().stream()
                .map(Hit::source)
                .toList();

        page.setTotal(total);
        page.setRecords(list);

        return page;
    }

    public String makeIndexName(String indexName){
        return INDEX_PREFIX + indexName;
    }


}
