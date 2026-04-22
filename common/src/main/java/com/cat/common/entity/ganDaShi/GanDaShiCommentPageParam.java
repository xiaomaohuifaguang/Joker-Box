package com.cat.common.entity.ganDaShi;


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
@Schema(name = "GanDaShiCommentPageParam", description = "评论查询分页查询")
@EqualsAndHashCode(callSuper = false)
public class GanDaShiCommentPageParam extends PageParam {

    @Schema(description = "帖子id")
    private String postId;

    @Schema(description = "主评论id")
    private String parentId;


}
