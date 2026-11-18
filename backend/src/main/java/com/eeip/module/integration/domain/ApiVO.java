package com.eeip.module.integration.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

/** 接口视图。 */
@Schema(description = "接口")
public record ApiVO(
        @Schema(description = "ID") String id,
        @Schema(description = "系统编码") String systemCode,
        @Schema(description = "接口编码") String code,
        @Schema(description = "名称") String name,
        @Schema(description = "状态") String status,
        @Schema(description = "当前版本") String version,
        @Schema(description = "方法") String method,
        @Schema(description = "上游路径") String path,
        @Schema(description = "绑定的连接器") List<String> connectorCodes,
        @Schema(description = "对外路径（已发布时）", example = "/api/open/v1/erp-core/create-order") String publicPath,
        @Schema(description = "描述") String description,
        @Schema(description = "更新时间") OffsetDateTime updatedAt
) {
}
