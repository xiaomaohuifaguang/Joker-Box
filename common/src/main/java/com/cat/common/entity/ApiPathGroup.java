package com.cat.common.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/***
 * <TODO description class purpose>
 * @title ApiPathGroup
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/16 14:20
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "ApiPathGroup", description = "api路径分组")
public class ApiPathGroup {

    @Schema(description = "分组名称")
    private String groupName;

    @Schema(description = "组下api路径")
    private List<ApiPath> apiPaths;


}
