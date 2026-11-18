package com.eeip.module.integration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

/**
 * 流程 DSL（对齐详细设计 06 章 §2）。
 *
 * <p>节点之间用 {@code next} 串联，{@code CHOICE} 节点用 {@code branches} 分流。</p>
 */
@Schema(description = "流程 DSL")
public record FlowDsl(

        @Schema(description = "DSL 版本", example = "1.0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "DSL 版本不能为空")
        String schemaVersion,

        @Schema(description = "流程编码（与元数据一致）", example = "demo-ping")
        String flowCode,

        @Schema(description = "流程名称", example = "连通性演示：调用 mock 并转字段")
        String name,

        @Schema(description = "触发配置")
        Trigger trigger,

        @Schema(description = "节点列表", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "流程至少需要一个节点")
        @Valid
        List<Node> nodes
) {

    /** 触发配置 */
    @Schema(description = "触发配置")
    public record Trigger(
            @Schema(description = "触发类型", example = "HTTP") String type,
            @Schema(description = "HTTP 方法", example = "POST") String method,
            @Schema(description = "路径", example = "/api/v1/demo/ping") String path,
            @Schema(description = "超时毫秒", example = "15000") Integer timeoutMs
    ) {
    }

    /**
     * 节点。
     *
     * <p>{@code config} 与 {@code branches} 按 type 取用：
     * HTTP_INVOKE 用 config.connectionCode，TRANSFORM 用 config.mapping，
     * CHOICE 用 branches。</p>
     */
    @Schema(description = "流程节点")
    public record Node(
            @Schema(description = "节点 ID，流程内唯一", example = "n1",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "节点 ID 不能为空")
            String id,

            @Schema(description = "节点类型", example = "HTTP_INVOKE",
                    allowableValues = {"START", "HTTP_INVOKE", "TRANSFORM", "CHOICE", "END"},
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "节点类型不能为空")
            String type,

            @Schema(description = "节点名称", example = "调用 mock 接口")
            String name,

            @Schema(description = "节点配置，随类型不同", example = "{\"connectionCode\":\"mock_echo\"}")
            Map<String, Object> config,

            @Schema(description = "下一节点 ID（CHOICE 之外必填）", example = "n2")
            String next,

            @Schema(description = "CHOICE 分支")
            List<Branch> branches
    ) {
    }

    /** CHOICE 分支：when 为条件表达式，otherwise 为兜底 */
    @Schema(description = "分支条件")
    public record Branch(
            @Schema(description = "条件表达式", example = "$resp.code == 200") String when,
            @Schema(description = "是否兜底分支", example = "false") Boolean otherwise,
            @Schema(description = "命中后跳转的节点", example = "n3") String next
    ) {
    }
}
