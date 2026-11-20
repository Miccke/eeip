package com.eeip.module.integration.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/** 映射预览请求。 */
@Schema(description = "映射预览请求")
public record MappingPreviewRequest(
        @Schema(description = "映射定义：目标字段 → 表达式（$ 开头）或常量",
                example = "{\"orderNo\":\"$.orderNo\",\"level\":\"$.customerLevel\"}")
        Map<String, Object> mapping,

        @Schema(description = "上游样本报文（用于取值预览）",
                example = "{\"orderNo\":\"SO-1\",\"customerLevel\":\"VIP\"}")
        Map<String, Object> input,

        @Schema(description = "额外的变量上下文，如 outputVar 或其他节点输出")
        Map<String, Object> vars
) {
}
