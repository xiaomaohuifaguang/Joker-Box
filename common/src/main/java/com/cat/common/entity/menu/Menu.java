package com.cat.common.entity.menu;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-01-07
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_menu")
@Schema(name = "Menu", description = "")
public class Menu implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "父级id 根路径 -1")
    private Integer parentId;

    @Schema(description = "路由")
    private String path;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")

    private LocalDateTime createTime = LocalDateTime.now();

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    private String userId;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "子菜单")
    @TableField(exist = false)
    private List<Menu> children = new ArrayList<>();

    @Schema(description = "类型 -1 后台 -2 前台")
    private Integer menuType;

    @Schema(description = "是否白名单 1是0否")
    private String whiteList;


    public boolean verify(){
        return StringUtils.hasText(this.name) && StringUtils.hasText(this.path);
    }




}