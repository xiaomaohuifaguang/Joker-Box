package com.cat.common.entity.auth;

import com.cat.common.entity.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.util.StringUtils;

import static com.cat.common.entity.CONSTANTS.ORG_PARENT;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "OrgPageParam", description = "org分页查询参数")
@EqualsAndHashCode(callSuper = false)
public class OrgPageParam extends PageParam {


    @Schema(description = "搜索")
    private String search;

    @Schema(description = "父级机构id")
    private String parentId;



    public void init() {
        if(!StringUtils.hasText(parentId)){
            this.parentId = String.valueOf(ORG_PARENT);
        }
    }


}
