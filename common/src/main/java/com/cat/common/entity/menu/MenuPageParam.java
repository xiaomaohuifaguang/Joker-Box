package com.cat.common.entity.menu;

import com.cat.common.entity.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "MenuPageParam", description = "菜单分页查询参数")
@EqualsAndHashCode(callSuper = false)
public class MenuPageParam extends PageParam {

    @Schema(description = "菜单类型 -2 前台 -1 后台")
    private String menuType;




}
