package com.cat.simple.config.opensearch;

import org.opensearch.client.opensearch.indices.CreateIndexRequest;

public interface IndexConfigurable {

    CreateIndexRequest createIndexRequest();

}
