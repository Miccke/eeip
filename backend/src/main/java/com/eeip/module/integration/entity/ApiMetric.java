package com.eeip.module.integration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/** 接口调用计量（分钟粒度）。 */
@Data
@TableName("t_api_metric")
public class ApiMetric {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long exposeId;

    private Long apiId;

    /** 分钟桶 */
    private OffsetDateTime bucket;

    private Long calls;

    private Long errors;

    private Long costMsSum;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
