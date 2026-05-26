package com.cat.common.entity.dynamicForm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "已发布表单及其历史版本")
public class DynamicFormPublishedVersion {

    @Schema(description = "表单id")
    private String formId;

    @Schema(description = "表单名称")
    private String formName;

    @Schema(description = "最新版本号")
    private String latestVersion;

    @Schema(description = "历史版本列表")
    private List<Version> versions;

    @Data
    @Schema(description = "表单版本信息")
    public static class Version {
        @Schema(description = "版本号")
        private String version;

        @Schema(description = "发布时间")
        private String publishTime;
    }
}