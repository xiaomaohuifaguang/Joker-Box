package com.cat.common.entity.ai.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/***
 * <TODO description class purpose>
 * @title ChatRequestPram
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/8/2 22:52
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "ChatRequestPram", description = "chat接口请求参数")
public class ChatRequestPram implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "会话id")
    private String dialogId;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "流式响应")
    private Boolean stream = false;



}
