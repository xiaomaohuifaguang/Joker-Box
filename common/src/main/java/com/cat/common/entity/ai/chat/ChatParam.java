package com.cat.common.entity.ai.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/***
 * <TODO description class purpose>
 * @title ChatParam
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/8/2 22:08
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "ChatParam", description = "openAi chat请求参数")
public class ChatParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "模型")
    private String model;

    @Schema(description = "消息列表")
    private List<Message> messages = new ArrayList<>();

    @Schema(description = "流式响应")
    private Boolean stream = false;


    public ChatParam setMessages(List<AiMessage> aiMessages) {
        this.messages = new ArrayList<>();
        aiMessages.forEach(m->this.messages.add(new Message().setRole(m.getRole()).setContent(m.getContent()).setName(m.getName())));
        return this;
    }
}
