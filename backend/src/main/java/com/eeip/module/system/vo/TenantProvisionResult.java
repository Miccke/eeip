package com.eeip.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 开通租户结果（初始密码只在此处返回一次，请立即保存）。
 */
@Schema(description = "开通租户结果")
public record TenantProvisionResult(

        @Schema(description = "租户 ID", example = "3")
        Long tenantId,

        @Schema(description = "租户编码", example = "SUPPLIER_B")
        String tenantCode,

        @Schema(description = "管理员用户名", example = "admin")
        String adminUsername,

        @Schema(description = "初始密码（仅本次返回）", example = "Kx7#mQ2pL9aT")
        String initialPassword,

        @Schema(description = "创建的角色数", example = "3")
        int roleCount,

        @Schema(description = "授予的权限点数", example = "32")
        int permissionCount
) {
}
