package com.eeip.module.integration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 流程触发器：由发布版本的 DSL 扫描生成。
 *
 * <p>调度线程按 {@code next_fire_at} 扫描到期的 SCHEDULE 触发器并执行流程。</p>
 */
@Data
@TableName("t_flow_trigger")
public class IntFlowTrigger {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long flowId;

    /** 绑定的发布版本 ID：未发布的修改不生效 */
    private Long versionId;

    private String flowCode;

    /** DSL 中的触发器节点 ID */
    private String nodeId;

    /** SCHEDULE / MQ / EVENT / HTTP */
    private String triggerType;

    /** 节点 config 快照（cron、timezone 等） */
    private String configJson;

    /** ACTIVE / PAUSED / OFFLINE */
    private String status;

    /** 下次触发时间 */
    private OffsetDateTime nextFireAt;

    private OffsetDateTime lastFireAt;

    /** SUCCESS / FAILED / SKIPPED */
    private String lastStatus;

    /** 最近一次实例 ID */
    private Long lastInstance;

    private Integer fireCount;

    /** 错过次数（休眠/宕机导致） */
    private Integer missCount;

    private String createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @TableLogic
    private Boolean deleted;
}
