package com.eeip.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求。
 */
@Schema(description = "登录请求")
public record LoginRequest(

        @Schema(description = "用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validation.username.required}")
        @Size(max = 64)
        String username,

        @Schema(description = "密码（明文传输，由 HTTPS 保护）", example = "Admin@123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validation.password.required}")
        @Size(max = 100)
        String password,

        @Schema(description = "租户编码，留空默认 GROUP（集团总部）", example = "GROUP")
        @Size(max = 32)
        String tenantCode
) {
}
