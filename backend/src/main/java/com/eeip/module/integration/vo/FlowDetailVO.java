package com.eeip.module.integration.vo;

import com.eeip.module.integration.dto.FlowDsl;
import io.swagger.v3.oas.annotations.media.Schema;

/** 流程详情（含 DSL）。 */
@Schema(description = "流程详情")
public record FlowDetailVO(

        @Schema(description = "流程 ID") String id,
        @Schema(description = "流程编码") String code,
        @Schema(description = "流程名称") String name,
        @Schema(description = "触发类型") String triggerType,
        @Schema(description = "状态") String status,
        @Schema(description = "当前版本号") String version,
        @Schema(description = "描述") String description,
        @Schema(description = "DSL") FlowDsl dsl
) {
}
