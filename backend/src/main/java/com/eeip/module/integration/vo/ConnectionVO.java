package com.eeip.module.integration.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * 连接列表项（不含任何凭证信息）。
 */
@Schema(description = "连接")
public record ConnectionVO(

        @Schema(description = "连接 ID", example = "1794221122334455")
        Long id,

        @Schema(description = "连接编码", example = "github_zen")
        String code,

        @Schema(description = "连接名称", example = "GitHub 公共 API")
        String name,

        @Schema(description = "类型", example = "HTTP")
        String type,

        @Schema(description = "目标地址", example = "https://api.github.com/zen")
        String endpoint,

        @Schema(description = "HTTP 方法", example = "GET")
        String method,

        @Schema(description = "认证方式", example = "NONE")
        String authType,

        @Schema(description = "是否已配置认证信息", example = "false")
        boolean hasAuth,

        @Schema(description = "状态", example = "1")
        Integer status,

        @Schema(description = "最近一次测试结果", example = "SUCCESS")
        String lastCheckStatus,

        @Schema(description = "最近一次测试耗时（毫秒）", example = "334")
        Integer lastCheckMs,

        @Schema(description = "最近一次测试失败原因", example = "Connection timed out")
        String lastCheckError,

        @Schema(description = "最近一次测试时间", example = "2026-11-17T22:10:00+08:00")
        OffsetDateTime lastCheckAt,

        @Schema(description = "备注", example = "用于演示")
        String remark
) {
}
