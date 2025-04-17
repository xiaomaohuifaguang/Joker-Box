package com.cat.common.entity.mail;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;
import java.io.Serial;
/**
 * <p>
 * 邮件记录
 * </p>
 *
 * @author xiaomaohuifaguang
 * @since 2025-04-17
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("cat_mail_info")
@Schema(name = "MailInfo", description = "邮件记录")
public class MailInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "邮件id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "收件人邮箱")
    private String toMail;

    @Schema(description = "主题")
    private String subject;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "变量")
    private String variable;

    @Schema(description = "发送时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime sendTime;
}