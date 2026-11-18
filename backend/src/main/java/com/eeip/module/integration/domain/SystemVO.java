package com.eeip.module.integration.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/** 系统视图（含连接器/接口数量）。 */
@Schema(description = "生产方系统")
public record SystemVO(
        @Schema(description = "ID") String id,
        @Schema(description = "编码") String code,
        @Schema(description = "名称") String name,
        @Schema(description = "环境") String env,
        @Schema(description = "主协议") String protocol,
        @Schema(description = "地址") String baseUrl,
        @Schema(description = "责任人") String owner,
        @Schema(description = "联系电话") String ownerPhone,
        @Schema(description = "状态") Integer status,
        @Schema(description = "连接器数") Integer connectorCount,
        @Schema(description = "接口数") Integer apiCount,
        @Schema(description = "备注") String remark,
        @Schema(description = "更新时间") OffsetDateTime updatedAt
) {
}
