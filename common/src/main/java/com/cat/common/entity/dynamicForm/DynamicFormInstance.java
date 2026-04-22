package com.cat.common.entity.dynamicForm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;
import java.io.Serial;
/**
 * <p>
 * 动态表单实例
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-05-06
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_dynamic_form_instance")
@Schema(name = "DynamicFormInstance", description = "动态表单实例")
public class DynamicFormInstance implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "动态表单实例id")
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @Schema(description = "表单id")
    private String formId;

    @Schema(description = "版本")
    private String version;

    @Schema(description = "逻辑删除")
    @TableLogic
    private String deleted;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "修改时间")@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime updateTime;
}