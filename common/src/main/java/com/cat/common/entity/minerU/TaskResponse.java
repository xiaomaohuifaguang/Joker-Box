package com.cat.common.entity.minerU;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(description = "MinerU 文件解析任务响应")
public class TaskResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务ID")
    @JsonProperty("task_id")
    private String taskId;

    @Schema(description = "任务状态：pending / running / success / failed")
    @JsonProperty("status")
    private String status;

    @Schema(description = "解析后端引擎：pipeline / vlm-engine / vlm-sglang-engine / hybrid-engine 等")
    @JsonProperty("backend")
    private String backend;

    @Schema(description = "上传的文件名列表（不含扩展名）")
    @JsonProperty("file_names")
    private List<String> fileNames;

    @Schema(description = "任务创建时间（UTC，ISO 8601）")
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @Schema(description = "任务开始处理时间，未开始则为 null")
    @JsonProperty("started_at")
    private OffsetDateTime startedAt;

    @Schema(description = "任务完成时间，未完成则为 null")
    @JsonProperty("completed_at")
    private OffsetDateTime completedAt;

    @Schema(description = "失败时的错误信息，无错误则为 null")
    @JsonProperty("error")
    private String error;

    @Schema(description = "查询任务状态的 URL")
    @JsonProperty("status_url")
    private String statusUrl;

    @Schema(description = "获取任务结果的 URL")
    @JsonProperty("result_url")
    private String resultUrl;

    @Schema(description = "排在当前任务前面的队列任务数")
    @JsonProperty("queued_ahead")
    private Integer queuedAhead;

    @Schema(description = "任务提交结果消息")
    @JsonProperty("message")
    private String message;
}
