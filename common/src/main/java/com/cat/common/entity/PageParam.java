package com.cat.common.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/***
 * <TODO description class purpose>
 * @title PageParam
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/12 23:50
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "PageParam", description = "分页查询参数")
public class PageParam {

    @Schema(description = "页大小")
    protected long size;

    @Schema(description = "页码")
    protected long current;


}
