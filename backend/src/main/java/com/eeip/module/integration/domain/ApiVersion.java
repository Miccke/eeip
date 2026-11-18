package com.eeip.module.integration.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 接口契约版本：方法、上游路径、报文结构。
 */
@Data
@TableName("t_api_version")
public class ApiVersion {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long apiId;

    /** 1.0.0 / 1.1.0 */
    private String version;

    private String method;

    /** 上游路径 */
    private String path;

    private String inputSchema;

    private String outputSchema;

    /** DRAFT / PUBLISHED / DEPRECATED */
    private String status;

    private String changeLog;

    private OffsetDateTime publishedAt;
    private String publishedBy;
    private String createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @TableLogic
    private Boolean deleted;
}
