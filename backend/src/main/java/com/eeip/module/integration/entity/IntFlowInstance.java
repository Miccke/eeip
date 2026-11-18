package com.eeip.module.integration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/** 流程执行实例。 */
@Data
@TableName("t_flow_instance")
public class IntFlowInstance {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private Long flowId;
    private Long versionId;
    private String flowCode;
    private String triggerType;
    private String status;
    private String inputJson;
    private String outputJson;
    private String error;
    private Integer costMs;
    private String traceId;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private String createdBy;

    @TableLogic
    private Boolean deleted;
}
