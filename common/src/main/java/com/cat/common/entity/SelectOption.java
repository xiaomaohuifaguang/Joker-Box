package com.cat.common.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/***
 * <TODO description class purpose>
 * @title SelectOption
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/7/19 0:54
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "SelectOption", description = "选择器项")
public class SelectOption {

    @Schema(description = "key")
    private Object key;
    @Schema(description = "value")
    private String value;

}
