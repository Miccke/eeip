package com.eeip.module.integration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/** 死信：执行失败的实例与原始入参，可重投或丢弃。 */
@Data
@TableName("t_flow_dead_letter")
public class IntFlowDeadLetter {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long instanceId;

    private Long flowId;

    private String flowCode;

    private String flowName;

    private Long versionId;

    /** 失败节点 */
    private String nodeId;

    private String nodeName;

    private String nodeType;

    private String error;

    /** 原始触发入参（重投时使用） */
    private String inputJson;

    private Integer attempts;

    /** PENDING / RETRIED / DISCARDED */
    private String status;

    /** 重投后产生的新实例 */
    private Long retriedInstance;

    private Integer retryCount;

    private String handledBy;

    private OffsetDateTime handledAt;

    private String remark;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    @TableLogic
    private Boolean deleted;
}
