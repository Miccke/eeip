package com.eeip.module.integration.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/** 运行统计（监控页顶部指标卡）。 */
@Schema(description = "实例统计")
public record InstanceStatsVO(
        @Schema(description = "总实例数") long total,
        @Schema(description = "成功数") long success,
        @Schema(description = "失败数") long failed,
        @Schema(description = "运行中") long running,
        @Schema(description = "成功率（百分比，保留 1 位）") double successRate,
        @Schema(description = "平均耗时（毫秒）") long avgCostMs,
        @Schema(description = "最大耗时（毫秒）") long maxCostMs,
        @Schema(description = "统计窗口（小时）") int hours
) {
}
