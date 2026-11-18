package com.eeip.module.integration.expose;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 接口登记/编辑请求。
 */
@Schema(description = "接口登记请求")
public record ApiExposeSaveRequest(

        @Schema(description = "接口编码", example = "erp-create-order", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "接口编码不能为空")
        @Pattern(regexp = "^[a-z][a-z0-9-]{2,63}$", message = "编码需小写字母开头，可含数字与短横线")
        String code,

        @Schema(description = "接口名称", example = "ERP 创建订单", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "接口名称不能为空")
        @Size(max = 128)
        String name,

        @Schema(description = "对外方法", example = "POST", allowableValues = {"GET", "POST", "PUT", "DELETE"})
        String method,

        @Schema(description = "对外路径（不含 /api/open 前缀）", example = "/orders", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "对外路径不能为空")
        @Pattern(regexp = "^/[a-zA-Z0-9_/{}-]+$", message = "路径需以 / 开头，可含字母数字、下划线、短横线与 {id}")
        String path,

        @Schema(description = "目标类型", example = "PROXY", allowableValues = {"PROXY", "FLOW"})
        String targetType,

        @Schema(description = "上游连接编码", example = "mock_echo")
        String connectionCode,

        @Schema(description = "上游路径模板", example = "/api/mock/orders/{id}")
        String upstreamPath,

        @Schema(description = "上游方法（空则沿用对外方法）", example = "GET")
        String upstreamMethod,

        @Schema(description = "追加请求头 JSON", example = "{\"X-From\":\"eeip\"}")
        String headersJson,

        @Schema(description = "鉴权方式", example = "API_KEY", allowableValues = {"NONE", "API_KEY"})
        String authType,

        @Schema(description = "API Key（authType=API_KEY 时使用）", example = "ak_2f9c...")
        String apiKey,

        @Schema(description = "QPS 限制，0 表示不限", example = "100")
        Integer qps,

        @Schema(description = "状态：1 启用，0 停用", example = "1")
        Integer status,

        @Schema(description = "版本号", example = "v1")
        String version,

        @Schema(description = "备注", example = "对渠道方开放")
        @Size(max = 256)
        String remark
) {
}
