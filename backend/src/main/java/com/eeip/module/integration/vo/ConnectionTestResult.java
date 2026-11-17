package com.eeip.module.integration.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * 连通性测试结果。
 */
@Schema(description = "连通性测试结果")
public record ConnectionTestResult(

        @Schema(description = "是否成功（HTTP 2xx）", example = "true")
        boolean success,

        @Schema(description = "HTTP 状态码", example = "200")
        Integer status,

        @Schema(description = "耗时（毫秒）", example = "334")
        long costMs,

        @Schema(description = "响应体摘要（截断至 500 字符）", example = "Keep it logically awesome.")
        String bodySnippet,

        @Schema(description = "响应头")
        Map<String, String> headers,

        @Schema(description = "失败原因", example = "连接超时")
        String error
) {
}
