package com.eeip.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * 租户列表项。
 */
@Schema(description = "租户")
public record TenantVO(

        @Schema(description = "租户 ID", example = "2")
        Long id,

        @Schema(description = "租户编码", example = "SUPPLIER_A")
        String code,

        @Schema(description = "租户名称", example = "供应商A")
        String name,

        @Schema(description = "类型：INTERNAL / PARTNER", example = "PARTNER")
        String tenantType,

        @Schema(description = "状态：1 启用，0 停用", example = "1")
        Integer status,

        @Schema(description = "联系人", example = "李经理")
        String contactName,

        @Schema(description = "联系电话", example = "021-88886666")
        String contactPhone,

        @Schema(description = "到期时间", example = "2027-12-31T00:00:00+08:00")
        OffsetDateTime expireAt,

        @Schema(description = "备注", example = "渠道合作方")
        String remark,

        @Schema(description = "创建时间", example = "2026-11-17T21:00:00+08:00")
        OffsetDateTime createdAt
) {
}
