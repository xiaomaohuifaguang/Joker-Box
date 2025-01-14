package com.cat.common.entity.menu;

import com.cat.common.entity.auth.ApiPathServer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "MenuAndApiPath", description = "菜单详细信息")
public class MenuAndApiPath {

    @Schema(description = "菜单信息")
    private Menu menu;

    @Schema(description = "api路径关系树")
    private List<ApiPathServer> apiPathTree;

}
