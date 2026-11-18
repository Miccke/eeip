package com.eeip.module.integration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 集成流程定义（元数据，DSL 存于版本表）。
 */
@Data
@TableName("t_flow")
public class IntFlow {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户（拦截器自动维护） */
    private Long tenantId;

    /** 流程编码，租户内唯一 */
    private String flowCode;

    /** 流程名称 */
    private String flowName;

    /** 触发方式：HTTP / MQ / SCHEDULE / EVENT */
    private String triggerType;

    /** 状态：DRAFT / PUBLISHED / OFFLINE */
    private String status;

    /** 当前生效版本 ID */
    private Long currentVersionId;

    private String description;

    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;

    @TableLogic
    private Boolean deleted;

    @Version
    private Integer version;
}
