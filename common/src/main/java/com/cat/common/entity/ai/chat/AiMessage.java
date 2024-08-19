package com.cat.common.entity.ai.chat;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.cat.common.entity.ai.chat.res.ChatResponse;
import com.cat.common.utils.JSONUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.Serial;
/**
 * <p>
 * 
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2024-08-02
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_ai_message")
@Schema(name = "AiMessage", description = "消息信息")
public class AiMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public AiMessage(ChatResponse chatResponse) {
        if(!ObjectUtils.isEmpty(chatResponse.getChoices().get(0).getMessage())){
            this.role =  chatResponse.getChoices().get(0).getMessage().getRole();
            setContent(chatResponse.getChoices().get(0).getMessage().getContent());
        }
        if(!ObjectUtils.isEmpty(chatResponse.getChoices().get(0).getDelta())){
            this.role =  chatResponse.getChoices().get(0).getDelta().getRole();
            setContent(chatResponse.getChoices().get(0).getDelta().getContent());
        }
    }

    @Schema(description = "消息id")
    @TableId(value = "id")
    private String id;

    @Schema(description = "会话id")
    private String dialogId;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "角色")
    private String role;

    @Schema(description = "参与者名称(可选)")
    private String name;

    @Schema(description = "逻辑删除 1 是 0 否")
    @TableLogic
    private String deleted = "0";

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime createTime = LocalDateTime.now();

    @Schema(description = "tokens")
    private long tokens = 0;

    @Schema(description = "原始请求/响应")
    private String original;

    public AiMessage setContent(String content) {
        this.content = content;
        this.tokens = this.content.length();
        return this;
    }

    public AiMessage appendContent(String append){
        return setContent(StringUtils.hasText(this.content) ? this.content+append : append);
    }

}