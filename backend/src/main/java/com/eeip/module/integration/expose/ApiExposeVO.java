package com.eeip.module.integration.expose;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/** 对外接口视图（不返回 apiKey 明文）。 */
@Schema(description = "对外接口")
public record ApiExposeVO(
        @Schema(description = "ID") String id,
        @Schema(description = "编码") String code,
        @Schema(description = "名称") String name,
        @Schema(description = "对外方法") String method,
        @Schema(description = "对外路径") String path,
        @Schema(description = "完整调用地址", example = "http://localhost:8080/api/open/orders") String fullPath,
        @Schema(description = "目标类型") String targetType,
        @Schema(description = "上游连接") String connectionCode,
        @Schema(description = "上游路径") String upstreamPath,
        @Schema(description = "鉴权方式") String authType,
        @Schema(description = "是否已配置 Key") boolean hasKey,
        @Schema(description = "QPS") Integer qps,
        @Schema(description = "状态") Integer status,
        @Schema(description = "版本") String version,
        @Schema(description = "备注") String remark,
        @Schema(description = "更新时间") OffsetDateTime updatedAt
) {
}
