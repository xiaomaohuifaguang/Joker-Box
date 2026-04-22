package com.cat.common.entity.dynamicForm;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * <p>
 *
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-04-30
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "DynamicFormOption", description = "表单项配置")
public class DynamicFormOption implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;


    @Schema(description = "标签")
    private String label;

    @Schema(description = "值")
    private String value;

    @Schema(description = "子集")
    private List<DynamicFormOption> children;

    public DynamicFormOption(String label, String value) {
        this.label = label;
        this.value = value;
    }


}
