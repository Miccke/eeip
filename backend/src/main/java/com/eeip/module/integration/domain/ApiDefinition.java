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

    /** 接口编号，如 API-SCM-0001 */
    private String apiNo;

    /** 接口编码，系统内唯一 */
    private String code;

    /** 协议：HTTP/SOAP/JDBC/MQ/FILE */
    private String protocol;

    /** SLA 等级：L1 核心 / L2 重要 / L3 一般 */
    private String slaLevel;

    /** 业务领域 */
    private String domainCode;

    /** 实现方式：CONNECTOR 直连 / FLOW 走流程 */
    private String targetType;

    /** target_type=FLOW 时绑定的流程编码 */
    private String flowCode;

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
