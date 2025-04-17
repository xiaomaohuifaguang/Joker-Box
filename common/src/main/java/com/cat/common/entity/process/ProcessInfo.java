package com.cat.common.entity.process;


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
@Schema(name = "ProcessInfo", description = "流程详细信息")
public class ProcessInfo {

    @Schema(description = "流程实例信息")
    private ProcessInstance processInstance;

    @Schema(description = "待处理 可用按钮类型")
    private List<String> handleButton;

    @Schema(description = "流程处理记录")
    private List<ProcessHandleInfo> handleInfos;

}
