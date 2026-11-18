package com.eeip.module.integration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 流程保存请求（新建 / 编辑共用）。
 */
@Schema(description = "流程保存请求")
public record FlowSaveRequest(

        @Schema(description = "流程编码（租户内唯一）", example = "demo-ping",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "流程编码不能为空")
        @Pattern(regexp = "^[a-z][a-z0-9-]{2,63}$", message = "编码需小写字母开头，可含数字与短横线")
        String code,

        @Schema(description = "流程名称", example = "连通性演示", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "流程名称不能为空")
        @Size(max = 128)
        String name,

        @Schema(description = "触发类型", example = "HTTP",
                allowableValues = {"HTTP", "MQ", "SCHEDULE", "EVENT"})
        String triggerType,

        @Schema(description = "描述", example = "调用 mock 接口并提取字段")
        @Size(max = 1024)
        String description,

        @Schema(description = "变更说明（发布时写入版本记录）", example = "首个版本")
        @Size(max = 2048)
        String changeLog,

        @Schema(description = "流程 DSL", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "DSL 不能为空")
        @Valid
        FlowDsl dsl
) {
}
