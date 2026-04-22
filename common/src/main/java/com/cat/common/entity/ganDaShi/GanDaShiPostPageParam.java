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
@Schema(name = "GanDaShiPostPageParam", description = "干大事论坛帖子分页查询参数")
@EqualsAndHashCode(callSuper = false)
public class GanDaShiPostPageParam extends PageParam {

    @Schema(description = "用户名")
    private String createUsername;

    @Schema(description = "用户id")
    private String userId;




}
