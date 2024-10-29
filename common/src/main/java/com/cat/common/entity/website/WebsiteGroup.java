package com.cat.common.entity.website;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/***
 * 网站收藏业务层
 * @title WebsiteGroup
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/10/27
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "WebsiteGroup", description = "网站收藏分组")
@EqualsAndHashCode(callSuper = false)
public class WebsiteGroup {

    @Schema(description = "组名称")
    private String groupName;

    @Schema(description = "子集")
    private List<Website> child;


}
