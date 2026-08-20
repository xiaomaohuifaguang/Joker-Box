package com.cat.common.entity.ai.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_chat_message")
@Schema(name = "ChatMessage", description = "ai会话消息表")
public class ChatMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "id")
    @TableId(type = IdType.AUTO)
    private Integer id;

    @Schema(description = "消息唯一标识(UUID)")
    private String messageId;

    @Schema(description = "会话唯一标识(UUID)")
    private String sessionId;

    @Schema(description = "角色: system, user, assistant, tool")
    private String role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "思考内容")
    private String reasonContent;

    @Schema(description = "该条消息消耗的Token数")
    private Integer tokenCount;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime createTime;




}
