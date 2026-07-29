package com.cat.common.entity.process.designer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "Position", description = "坐标信息")
public class Position {

    @Schema(description = "x轴")
    private Double x;
    @Schema(description = "y轴")
    private Double y;
}
