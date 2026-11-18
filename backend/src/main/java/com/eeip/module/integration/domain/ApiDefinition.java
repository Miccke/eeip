package com.eeip.module.integration.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 接口台账：某系统提供了哪些能力（身份层，不含具体契约）。
 */
@Data
@TableName("t_api")
public class ApiDefinition {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 所属系统编码 */
    private String systemCode;

    /** 接口编码，系统内唯一 */
    private String code;

    private String name;

    /** DRAFT / PUBLISHED / OFFLINE */
    private String status;

    private Long currentVersionId;

    private String description;

    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;

    @TableLogic
    private Boolean deleted;

    private Integer versionLock;
}
