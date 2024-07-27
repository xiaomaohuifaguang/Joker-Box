package com.cat.common.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/***
 * <TODO description class purpose>
 * @title ApiPathPageParam
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/18 22:40
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "ApiPathPageParam", description = "api分页查询参数")
@EqualsAndHashCode(callSuper = false)
public class ApiPathPageParam extends PageParam{

    @Schema(description = "搜索")
    private String search;

    @Schema(description = "角色id")
    private String roleId;

    @Schema(description = "服务名称")
    private String server;

    @Schema(description = "分组名称")
    private String groupName;


}
