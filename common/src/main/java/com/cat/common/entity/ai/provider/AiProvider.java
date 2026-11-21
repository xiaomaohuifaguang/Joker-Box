package com.cat.common.entity.ai.provider;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.io.Serial;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;




@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(description = "ai供应商")
@JsonInclude(JsonInclude.Include.NON_NULL)
@TableName("cat_ai_provider")
public class AiProvider implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @Schema(description = "供应商id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @Schema(description = "供应商名称")
    private String name;
    @Schema(description = "供应商baseurl")
    @TableField("base_url")
    private String baseUrl;

}