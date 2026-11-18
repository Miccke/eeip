package com.eeip.module.integration.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** DSL 校验结果。 */
@Schema(description = "DSL 校验结果")
public record FlowValidateResult(

        @Schema(description = "是否通过", example = "true") boolean valid,

        @Schema(description = "错误列表（有错误则不通过）") List<Issue> errors,

        @Schema(description = "告警列表（不影响发布）") List<Issue> warnings,

        @Schema(description = "节点数", example = "4") int nodeCount,

        @Schema(description = "DSL 摘要", example = "9f2c1a7b3e04...") String dslHash
) {

    /** 单条问题 */
    @Schema(description = "校验问题")
    public record Issue(
            @Schema(description = "问题码", example = "FLOW_CYCLE") String code,
            @Schema(description = "说明", example = "存在环：n2 → n3 → n2") String message,
            @Schema(description = "相关节点", example = "n2") String nodeId
    ) {
    }
}
