package com.eeip.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * 审计日志列表项。
 */
@Schema(description = "审计日志")
public record AuditLogVO(

        @Schema(description = "日志 ID", example = "1794221122334455")
        Long id,

        @Schema(description = "动作编码", example = "USER_CREATE")
        String action,

        @Schema(description = "动作中文名", example = "新增用户")
        String actionName,

        @Schema(description = "资源类型", example = "USER")
        String resourceType,

        @Schema(description = "资源标识", example = "1004")
        String resourceId,

        @Schema(description = "操作人 ID（工号）", example = "1001")
        String operatorId,

        @Schema(description = "操作人姓名", example = "王工")
        String operatorName,

        @Schema(description = "请求方法与路径", example = "POST /api/v1/system/users")
        String endpoint,

        @Schema(description = "请求参数（已脱敏）", example = "[{\"username\":\"dev02\"}]")
        String requestParams,

        @Schema(description = "执行结果", example = "SUCCESS")
        String result,

        @Schema(description = "失败原因", example = "用户名已存在")
        String errorMsg,

        @Schema(description = "耗时（毫秒）", example = "18")
        Integer costMs,

        @Schema(description = "客户端 IP", example = "10.20.30.40")
        String clientIp,

        @Schema(description = "发生时间", example = "2026-11-17T21:30:00+08:00")
        OffsetDateTime occurredAt
) {
}
