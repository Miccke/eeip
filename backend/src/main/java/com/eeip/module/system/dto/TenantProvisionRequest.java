package com.eeip.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 开通租户请求：一次调用完成"建租户 + 建角色 + 建管理员"。
 */
@Schema(description = "开通租户请求")
public record TenantProvisionRequest(

        @Schema(description = "租户编码，登录时使用", example = "SUPPLIER_B",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validation.tenant.code.required}")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,31}$", message = "{validation.code.pattern}")
        String code,

        @Schema(description = "租户名称", example = "供应商B", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{validation.tenant.name.required}")
        @Size(max = 128)
        String name,

        @Schema(description = "类型：INTERNAL 内部 / PARTNER 合作方", example = "PARTNER",
                allowableValues = {"INTERNAL", "PARTNER"})
        String tenantType,

        @Schema(description = "管理员用户名", example = "admin")
        @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{2,31}$", message = "{validation.username.pattern}")
        String adminUsername,

        @Schema(description = "管理员姓名", example = "供应商B管理员")
        @Size(max = 64)
        String adminNickname,

        @Schema(description = "初始密码；留空则随机生成 12 位并仅在响应中返回一次")
        @Size(min = 8, max = 64)
        String adminPassword,

        @Schema(description = "联系人", example = "张三")
        @Size(max = 64)
        String contactName,

        @Schema(description = "联系电话", example = "021-88887777")
        @Size(max = 32)
        String contactPhone,

        @Schema(description = "备注", example = "渠道合作方")
        @Size(max = 256)
        String remark
) {
}
