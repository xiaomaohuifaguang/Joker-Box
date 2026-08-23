package com.cat.common.entity.ai.chat;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "QAMessage", description = "问答")
@JsonInclude(JsonInclude.Include.NON_NULL) // 序列化时忽略 null 值
@JsonIgnoreProperties(ignoreUnknown = true) // 防止 OpenSearch 返回多余字段导致反序列化报错
public class QAMessage implements Serializable {

    public static final String INDEX = "qa";

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "QA_id")
    private String id;

    @Schema(description = "会话唯一标识(UUID)")
    private String sessionId;

    @Schema(description = "消息唯一标识(UUID)")
    private String questionMessageId;

    @Schema(description = "问")
    private String question;

    @Schema(description = "问向量化")
    private List<Float> questionEmbeddings;

    @Schema(description = "消息唯一标识(UUID)")
    private String answerMessageId;

    @Schema(description = "答")
    private String answer;

    @Schema(description = "答向量化")
    private List<Float> answerEmbeddings;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone ="GMT+8")
    private LocalDateTime createTime;



}
