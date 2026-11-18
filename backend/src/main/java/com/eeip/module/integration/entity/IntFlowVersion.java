package com.eeip.module.integration.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.eeip.common.mybatis.JsonbTypeHandler;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 流程版本：DSL 全文 + 摘要哈希。
 */
@Data
@TableName(value = "t_flow_version", autoResultMap = true)
public class IntFlowVersion {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long flowId;

    /** 版本号，如 1.0.0 */
    private String version;

    /** DSL 全文（JSON 字符串，存 jsonb 列；写入需经 JsonbTypeHandler） */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String dslJson;

    /** SHA-256，用于变更检测与运行时缓存 key */
    private String dslHash;

    /** 节点数，列表页展示用 */
    private Integer nodeCount;

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

    /** 乐观锁字段（表结构预留，暂不使用 @Version 以免与流程自身的 version 混淆） */
    private Integer versionLock;
}
