package com.cat.common.entity.auth;


import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "OrgTree", description = "组织机构树")
public class OrgTree implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "组织id")
    private Integer id;

    @Schema(description = "组织机构名称")
    private String name;

    @Schema(description = "父级机构id")
    private Integer parentId;

    @Schema(description = "父级机构名称")
    private String parentName;


    @Schema(description = "子机构")
    private List<OrgTree> children = new ArrayList<>();


    public void addChild(OrgTree child) {
        this.children.add(child);
    }



    public static List<OrgTree> getChildren(Integer parentId,String parentName ,Map<Integer, List<Org>> map) {
        List<OrgTree> children = new ArrayList<>();

        List<Org> orgs = map.get(parentId);
        if(CollectionUtils.isEmpty(orgs)) {
            return children;
        }
        for (Org org : orgs) {
            OrgTree orgTree = new OrgTree()
                    .setId(org.getId())
                    .setName(org.getName())
                    .setParentId(parentId)
                    .setParentName(parentName);
            orgTree.setChildren(getChildren(org.getId(),org.getName(),map));
            children.add(orgTree);
        }
        return children;
    }



}
