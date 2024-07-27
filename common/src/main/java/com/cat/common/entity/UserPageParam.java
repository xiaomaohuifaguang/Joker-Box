package com.cat.common.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/***
 * <TODO description class purpose>
 * @title UserPageParam
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/18 17:09
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "UserPageParam", description = "用户分页查询参数")
@EqualsAndHashCode(callSuper = false)
public class UserPageParam extends PageParam{

    @Schema(description = "搜索")
    protected String search;

    @Schema(description = "角色id")
    private String roleId;

}
