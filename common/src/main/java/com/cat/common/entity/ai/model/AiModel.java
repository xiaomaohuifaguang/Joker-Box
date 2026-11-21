package com.cat.common.entity.ai.model;

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
/**
 * <p>
 * 
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-12-20
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_ai_model")
@Schema(name = "AiModel", description = "")
public class AiModel implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "id")
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "模型")
    private String model;

    @Schema(description = "渠道id")
    private Integer channelId;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "向量维度")
    private Integer dimension;

    @Schema(description = "基础URL")
    private String baseUrl;

    @Schema(description = "图像理解")
    private Boolean vision;

    @Schema(description = "completions请求路径")
    private String completionsPath;

    @Schema(description = "embeddings请求路径")
    private String embeddingsPath;

    @Schema(description = "API密钥")
    private String apiKey;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "创建人")
    private String userId;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "供应商名称")
    @TableField(exist = false)
    private String providerName;

    @Schema(description = "供应商BaseUrl")
    @TableField(exist = false)
    private String providerBaseUrl;


    @Schema(description = "渠道名称")
    @TableField(exist = false)
    private String channelName;

    @Schema(description = "渠道BaseUrl")
    @TableField(exist = false)
    private String channelBaseUrl;

    @Schema(description = "渠道API_KEY")
    @TableField(exist = false)
    private String channelApiKey;


    public String getUseBaseUrl(){
        return StringUtils.hasText(baseUrl) ? baseUrl : StringUtils.hasText(channelBaseUrl) ? channelBaseUrl : providerBaseUrl;
    }

    public String getUseApiKey(){
        return StringUtils.hasText(apiKey) ? apiKey : channelApiKey;
    }

}