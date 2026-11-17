package com.eeip.module.system.query;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 审计日志查询条件。
 *
 * @param pageNum      页码，从 1 开始
 * @param pageSize     每页条数，最大 200
 * @param operator     操作人（工号或姓名，模糊匹配）
 * @param action       动作编码，精确匹配
 * @param result       结果：SUCCESS / FAILED
 * @param resourceType 资源类型，如 USER / ROLE
 * @param startTime    起始时间（ISO 8601，含时区）
 * @param endTime      结束时间（ISO 8601，含时区）
 */
@Schema(description = "审计日志查询条件")
public record AuditLogQuery(
        @Schema(description = "页码，从 1 开始", example = "1", defaultValue = "1")
        Long pageNum,

        @Schema(description = "每页条数，最大 200", example = "20", defaultValue = "20")
        Long pageSize,

        @Schema(description = "操作人（工号或姓名，模糊匹配）", example = "1001")
        String operator,

        @Schema(description = "动作编码，精确匹配", example = "USER_CREATE")
        String action,

        @Schema(description = "执行结果", example = "SUCCESS", allowableValues = {"SUCCESS", "FAILED"})
        String result,

        @Schema(description = "资源类型", example = "USER")
        String resourceType,

        @Schema(description = "起始时间（ISO 8601）", example = "2026-11-01T00:00:00+08:00")
        String startTime,

        @Schema(description = "结束时间（ISO 8601）", example = "2026-11-30T23:59:59+08:00")
        String endTime
) {
    public long safePageNum() {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    public long safePageSize() {
        if (pageSize == null || pageSize < 1) return 20;
        return Math.min(pageSize, 200);
    }
}
