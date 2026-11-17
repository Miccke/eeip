package com.eeip.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 角色分配权限请求（全量覆盖）。
 */
@Schema(description = "角色授权请求")
public record AssignPermRequest(

        @Schema(description = "权限 ID 列表（全量覆盖，传空数组表示清空权限）",
                example = "[3001, 3100, 3101]", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{validation.perm.required}")
        List<Long> permissionIds
) {
}
