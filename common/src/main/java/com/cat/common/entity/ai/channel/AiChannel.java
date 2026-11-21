package com.cat.common.entity.ai.channel;

import com.cat.common.entity.ai.model.AiModel;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.io.Serial;
import java.util.List;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;




@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(description = "ai模型渠道")
@JsonInclude(JsonInclude.Include.NON_NULL)
@TableName("cat_ai_channel")
public class AiChannel implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @Schema(description = "渠道id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @Schema(description = "渠道名称")
    private String name;
    @Schema(description = "供应商id")
    @TableField("provider_id")
    private Integer providerId;
    @Schema(description = "自定义baseurl")
    @TableField("base_url")
    private String baseUrl;
    @Schema(description = "API KEY")
    @TableField("api_key")
    private String apiKey;

    @Schema(description = "是否自定义渠道")
    @TableField(exist = false)
    private Boolean isCustom;

    @Schema(description = "供应商名称")
    @TableField(exist = false)
    private String providerName;

    @Schema(description = "供应商BaseUrl")
    @TableField(exist = false)
    private String providerBaseUrl;


    @Schema(description = "模型列表")
    @TableField(exist = false)
    private List<AiModel> models;


}