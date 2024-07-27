package com.cat.common.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/***
 * <TODO description class purpose>
 * @title RoleAndApiPath
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/16 21:37
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "RoleAndApiPath", description = "角色详细信息")
public class RoleAndApiPath {

    @Schema(description = "角色信息")
    private Role role;

    @Schema(description = "api路径关系树")
    private List<ApiPathServer> apiPathTree;


}
