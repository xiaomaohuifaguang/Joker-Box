package com.cat.common.entity.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "RoundRecordVO", description = "轮次记录")
public class RoundRecordVO {

    @Schema(description = "轮次")
    private Integer round;

    @Schema(description = "处理记录")
    private List<ProcessHandleInfo> handleInfos;


}
