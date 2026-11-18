package com.eeip.module.integration.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 系统注册/编辑请求。 */
@Schema(description = "系统保存请求")
public record SystemSaveRequest(
        @Schema(description = "系统编码（大写或小写短横线，同环境唯一）", example = "erp-core",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "系统编码不能为空")
        @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9-]{1,63}$", message = "编码需字母开头，可含数字与短横线")
        String code,

        @Schema(description = "系统名称", example = "ERP 核心系统", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "系统名称不能为空") @Size(max = 128) String name,

        @Schema(description = "环境", example = "PROD", allowableValues = {"DEV", "TEST", "PROD"})
        String env,

        @Schema(description = "主协议", example = "HTTP", allowableValues = {"HTTP", "SOAP", "JDBC", "MQ", "FILE"})
        String protocol,

        @Schema(description = "默认访问地址", example = "http://erp.internal/api") @Size(max = 256) String baseUrl,
        @Schema(description = "责任人", example = "张三") @Size(max = 64) String owner,
        @Schema(description = "联系电话", example = "13800000000") @Size(max = 32) String ownerPhone,
        @Schema(description = "状态：1 启用 0 停用") Integer status,
        @Schema(description = "备注") @Size(max = 256) String remark
) {
}
