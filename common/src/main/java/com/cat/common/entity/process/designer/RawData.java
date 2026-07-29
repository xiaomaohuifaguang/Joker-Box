package com.cat.common.entity.process.designer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "RawData", description = "画布数据")
public class RawData {

    @Schema(description = "节点信息")
    private List<Node> nodes;
    @Schema(description = "连线信息")
    private List<Edge> edges;

}
