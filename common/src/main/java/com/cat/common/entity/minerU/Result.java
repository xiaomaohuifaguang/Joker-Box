package com.cat.common.entity.minerU;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(description = "解析结果")
public class Result implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "markdown格式")
    private String md_content;

    @Schema(description = "images")
    private Map<String, String> images;

    @Schema(description = "middle_json")
    private String middle_json;

    @Schema(description = "model_output")
    private String model_output;

    @Schema(description = "content_list")
    private String content_list;


}
