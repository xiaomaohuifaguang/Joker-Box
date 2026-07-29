package com.cat.common.entity.process.designer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(name = "Node", description = "节点信息")
public class Node {

    @Schema(description = "节点id")
    private String id;
    @Schema(description = "节点类型")
    private String type;
    @Schema(description = "节点坐标")
    private Position position;
    @Schema(description = "节点数据")
    private Map<String, Object> data; // data 里的业务字段可能多变，这里用 Map 即可



}
