package com.cat.common.entity.auth;

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
@Schema(name = "ApiPathServerGroup", description = "api路径服务分组")
public class ApiPathServer {

    @Schema(description = "服务application.name")
    private String server;


    @Schema(description = "服务下分组")
    private List<ApiPathGroup> groups;


}
