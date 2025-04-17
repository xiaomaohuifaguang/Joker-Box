package com.cat.common.entity.statisticalCenter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "ChartData", description = "折线图数据")
public class ChartData {

    @Schema(description = "名称集")
    List<String> xdata;

    @Schema(description = "数据集")
    List<Object> ydata;


}
