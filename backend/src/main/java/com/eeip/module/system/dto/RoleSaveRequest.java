package com.eeip.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 角色新增/编辑请求。
 */
@Schema(description = "角色保存请求")
public record RoleSaveRequest(

        @Schema(description = "角色编码，大写蛇形且唯一", example = "ORDER_ADMIN",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validation.role.code.required}")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,31}$", message = "{validation.code.pattern}")
        String code,

        @Schema(description = "角色名称", example = "订单组管理员", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validation.role.name.required}")
        @Size(max = 64)
        String name,

        @Schema(description = "数据范围：ALL 全部 / TENANT 本租户 / PROJECT 同部门 / SELF 仅本人",
                example = "SELF", allowableValues = {"ALL", "TENANT", "PROJECT", "SELF"})
        @Size(max = 16)
        String dataScope,

        @Schema(description = "排序值，越小越靠前", example = "99")
        Integer sort,

        @Schema(description = "状态：1 启用，0 停用", example = "1")
        Integer status,

        @Schema(description = "备注", example = "订单域日常维护")
        @Size(max = 256)
        String remark
) {
}
