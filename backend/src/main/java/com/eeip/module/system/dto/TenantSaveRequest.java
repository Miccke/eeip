package com.eeip.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * 租户新增/编辑请求。
 */
@Schema(description = "租户保存请求")
public record TenantSaveRequest(

        @Schema(description = "租户编码，大写字母/数字/下划线，登录时使用", example = "SUPPLIER_B",
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

        @Schema(description = "状态：1 启用，0 停用", example = "1")
        Integer status,

        @Schema(description = "联系人", example = "张三")
        @Size(max = 64)
        String contactName,

        @Schema(description = "联系电话", example = "021-88886666")
        @Size(max = 32)
        String contactPhone,

        @Schema(description = "到期时间", example = "2027-12-31T00:00:00+08:00")
        OffsetDateTime expireAt,

        @Schema(description = "备注", example = "渠道合作方")
        @Size(max = 256)
        String remark
) {
}
