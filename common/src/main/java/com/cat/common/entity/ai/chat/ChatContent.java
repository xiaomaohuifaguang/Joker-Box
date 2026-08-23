package com.cat.common.entity.ai.chat;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_chat_message")
@Schema(name = "ChatMessage", description = "ai会话消息表")
public class ChatContent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


    private String type;

    private String content;


}
