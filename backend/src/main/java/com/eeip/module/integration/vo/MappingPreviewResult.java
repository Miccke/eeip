package com.eeip.module.integration.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/** 映射预览结果。 */
@Schema(description = "映射预览结果")
public record MappingPreviewResult(
        @Schema(description = "求值后的目标报文") Map<String, Object> output,
        @Schema(description = "每个字段的求值结果（含是否取到值）") List<FieldResult> fields,
        @Schema(description = "整体错误（映射为空等）") String error
) {
    /** 单字段结果 */
    @Schema(description = "字段求值结果")
    public record FieldResult(
            @Schema(description = "目标字段") String target,
            @Schema(description = "表达式或常量") String expr,
            @Schema(description = "求值结果（JSON 字符串）") String value,
            @Schema(description = "是否取到了值") boolean resolved
    ) {
    }
}
