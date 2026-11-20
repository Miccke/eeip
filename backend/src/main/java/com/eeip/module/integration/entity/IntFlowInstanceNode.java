package com.eeip.module.integration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/** 节点执行记录。 */
@Data
@TableName("t_flow_instance_node")
public class IntFlowInstanceNode {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private Long instanceId;
    private Integer seq;
    private String nodeId;
    private String nodeType;
    private String nodeName;
    private String status;
    private String inputJson;
    private String outputJson;
    private String error;
    /** 本节点实际执行次数（含重试） */
    private Integer attempts;

    private Integer costMs;
    private OffsetDateTime startedAt;
}
