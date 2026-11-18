package com.eeip.module.system.app;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/** 应用视图（不含密钥）。 */
@Schema(description = "接入应用")
public record AppClientVO(
        @Schema(description = "ID") String id,
        @Schema(description = "应用标识", example = "ak_LZ7f2Kp9") String appKey,
        @Schema(description = "应用名称", example = "A 系统") String appName,
        @Schema(description = "负责人") String owner,
        @Schema(description = "状态") Integer status,
        @Schema(description = "到期时间") OffsetDateTime expireAt,
        @Schema(description = "IP 白名单") String allowedIps,
        @Schema(description = "已授权接口数", example = "3") Integer grantedCount,
        @Schema(description = "备注") String remark,
        @Schema(description = "创建时间") OffsetDateTime createdAt
) {
}
