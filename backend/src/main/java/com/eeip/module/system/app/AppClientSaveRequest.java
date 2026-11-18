package com.eeip.module.system.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/** 应用保存请求。 */
@Schema(description = "应用保存请求")
public record AppClientSaveRequest(
        @Schema(description = "应用标识（留空自动生成）", example = "ak_LZ7f2Kp9") String appKey,
        @Schema(description = "应用名称", example = "A 系统", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "应用名称不能为空") @Size(max = 128) String appName,
        @Schema(description = "负责人", example = "张三") @Size(max = 64) String owner,
        @Schema(description = "状态：1 启用 0 停用") Integer status,
        @Schema(description = "到期时间") OffsetDateTime expireAt,
        @Schema(description = "IP 白名单，逗号分隔", example = "10.0.0.1,10.0.0.2") @Size(max = 512) String allowedIps,
        @Schema(description = "备注") @Size(max = 256) String remark
) {
}
