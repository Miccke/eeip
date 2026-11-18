package com.eeip.module.integration.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 接口保存请求（接口台账 + 当前契约）。 */
@Schema(description = "接口保存请求")
public record ApiSaveRequest(
        @Schema(description = "所属系统编码", example = "erp-core", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "所属系统不能为空") String systemCode,

        @Schema(description = "接口编码（系统内唯一）", example = "create-order",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "接口编码不能为空")
        @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9-]{1,63}$", message = "编码需字母开头，可含数字与短横线")
        String code,

        @Schema(description = "接口名称", example = "创建订单", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "接口名称不能为空") @Size(max = 128) String name,

        @Schema(description = "HTTP 方法", example = "POST", allowableValues = {"GET", "POST", "PUT", "DELETE"})
        String method,

        @Schema(description = "上游路径", example = "/orders", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "上游路径不能为空") @Size(max = 512) String path,

        @Schema(description = "绑定的连接器编码（可多个，主/备）", example = "[\"main\",\"backup\"]")
        List<String> connectorCodes,

        @Schema(description = "协议", example = "HTTP", allowableValues = {"HTTP", "SOAP", "JDBC", "MQ", "FILE"})
        String protocol,

        @Schema(description = "SLA 等级", example = "L1", allowableValues = {"L1", "L2", "L3"})
        String slaLevel,

        @Schema(description = "实现方式", example = "CONNECTOR", allowableValues = {"CONNECTOR", "FLOW"})
        String targetType,

        @Schema(description = "targetType=FLOW 时绑定的流程编码", example = "demo-run-order")
        String flowCode,

        @Schema(description = "变更说明", example = "新增库存字段") @Size(max = 2048) String changeLog,

        @Schema(description = "描述") @Size(max = 1024) String description
) {
}
