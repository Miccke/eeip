package com.eeip.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 审计动作字典项（用于前端筛选下拉）。
 */
@Schema(description = "审计动作")
public record AuditActionVO(
        @Schema(description = "动作编码", example = "USER_CREATE") String code,
        @Schema(description = "动作中文名", example = "新增用户") String name
) {
}
