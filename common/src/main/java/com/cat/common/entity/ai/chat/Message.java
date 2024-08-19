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
 * @title Message
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/8/2 22:10
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "Message", description = "chat请求消息")
public class Message implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The contents of the system message.
     */
    @Schema(description = "内容")
    private String content;

    /**
     * The role of the messages author, in this case system.
     */
    @Schema(description = "角色")
    private String role;

    /**
     * An optional name for the participant. Provides the model information to differentiate between participants of the same role.
     */
    @Schema(description = "参与者名称(可选)")
    private String name;




}
