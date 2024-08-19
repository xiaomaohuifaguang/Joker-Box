package com.cat.common.entity.auth;

import com.cat.common.entity.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/***
 * <TODO description class purpose>
 * @title RolePageParam
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/13 23:25
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "RolePageParam", description = "角色分页查询参数")
@EqualsAndHashCode(callSuper = false)
public class RolePageParam extends PageParam {

    @Schema(description = "搜索")
    protected String search;
}
