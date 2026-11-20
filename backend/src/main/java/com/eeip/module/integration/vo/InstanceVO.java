package com.eeip.module.integration.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/** 实例列表项（含流程名称，避免前端再查一次）。 */
@Schema(description = "流程实例")
public record InstanceVO(
        @Schema(description = "实例 ID") String id,
        @Schema(description = "流程 ID") String flowId,
        @Schema(description = "流程编码") String flowCode,
        @Schema(description = "流程名称") String flowName,
        @Schema(description = "版本 ID") String versionId,
        @Schema(description = "状态：RUNNING/SUCCESS/FAILED/SKIPPED") String status,
        @Schema(description = "触发方式：MANUAL/SCHEDULE/HTTP/MQ/EVENT") String triggerType,
        @Schema(description = "耗时（毫秒）") Integer costMs,
        @Schema(description = "链路 ID") String traceId,
        @Schema(description = "开始时间") OffsetDateTime startedAt,
        @Schema(description = "结束时间") OffsetDateTime finishedAt,
        @Schema(description = "错误信息") String error,
        @Schema(description = "节点数") Integer nodeCount
) {
}
