package com.cat.common.entity.process.designer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(name = "Edge", description = "连线信息")
public class Edge {

    @Schema(description = "连线id")
    private String id;
    @Schema(description = "连线入边节点id")
    private String source;
    @Schema(description = "连线出边节点id")
    private String target;
    @Schema(description = "连线出数据")
    private Map<String, Object> data; // 用于接收条件表达式等


}
