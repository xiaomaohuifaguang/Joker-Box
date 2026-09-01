package com.cat.common.entity.minerU;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "ParseResponse", description = "file_parse解析响应")
public class ParseResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "解析后端引擎：pipeline / vlm-engine / vlm-sglang-engine / hybrid-engine 等")
    @JsonProperty("backend")
    private String backend;

    @Schema(description = "minerU版本")
    @JsonProperty("version")
    private String version;

    @Schema(description = "解析结果集")
    @JsonProperty("results")
    private Map<String, Result> results;



}
