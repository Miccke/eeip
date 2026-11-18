package com.eeip.module.integration.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/** 流程列表项。 */
@Schema(description = "流程")
public record FlowVO(

        @Schema(description = "流程 ID", example = "2122693191068532737") String id,
        @Schema(description = "流程编码", example = "demo-ping") String code,
        @Schema(description = "流程名称", example = "连通性演示") String name,
        @Schema(description = "触发类型", example = "HTTP") String triggerType,
        @Schema(description = "状态", example = "DRAFT") String status,
        @Schema(description = "当前版本号", example = "1.0.0") String version,
        @Schema(description = "节点数", example = "4") Integer nodeCount,
        @Schema(description = "DSL 摘要（前 12 位）", example = "9f2c1a7b3e04") String dslHash,
        @Schema(description = "描述", example = "调用 mock 接口") String description,
        @Schema(description = "更新时间", example = "2026-11-18T19:00:00+08:00") OffsetDateTime updatedAt
) {
}
