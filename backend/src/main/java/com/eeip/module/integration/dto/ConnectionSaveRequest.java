package com.eeip.module.integration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 连接新增/编辑请求。
 */
@Schema(description = "连接保存请求")
public record ConnectionSaveRequest(

        @Schema(description = "连接编码（租户内唯一，小写蛇形）", example = "github_zen",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validation.conn.code.required}")
        @Pattern(regexp = "^[a-z][a-z0-9_]{1,63}$", message = "{validation.conn.code.pattern}")
        String code,

        @Schema(description = "连接名称", example = "GitHub 公共 API", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validation.conn.name.required}")
        @Size(max = 128)
        String name,

        @Schema(description = "目标地址（http/https）", example = "https://api.github.com/zen",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validation.conn.endpoint.required}")
        @Pattern(regexp = "^https?://.+", message = "{validation.endpoint.pattern}")
        @Size(max = 512)
        String endpoint,

        @Schema(description = "HTTP 方法", example = "GET", allowableValues = {"GET", "POST", "PUT", "DELETE"})
        String method,

        @Schema(description = "认证方式", example = "NONE",
                allowableValues = {"NONE", "BEARER", "BASIC", "API_KEY"})
        String authType,

        @Schema(description = "认证配置：BEARER 传 {\"token\":\"...\"}；BASIC 传 {\"username\":\"..\",\"password\":\"..\"}；API_KEY 传 {\"header\":\"X-Api-Key\",\"value\":\"..\"}",
                example = "{\"token\":\"ghp_xxx\"}")
        String authConfig,

        @Schema(description = "自定义请求头 JSON", example = "{\"Accept\":\"text/plain\"}")
        String headersJson,

        @Schema(description = "超时毫秒", example = "5000")
        Integer timeoutMs,

        @Schema(description = "状态：1 启用，0 停用", example = "1")
        Integer status,

        @Schema(description = "所属系统编码", example = "erp-core")
        String systemCode,

        @Schema(description = "协议", example = "HTTP")
        String protocol,

        @Schema(description = "备注", example = "用于演示的公开接口")
        @Size(max = 256)
        String remark
) {
}
