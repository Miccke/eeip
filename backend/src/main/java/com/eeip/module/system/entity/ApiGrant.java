package com.eeip.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 接口授权：某个应用可调用某个对外接口。
 */
@Data
@TableName("t_api_grant")
public class ApiGrant {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long clientId;

    private Long apiId;

    private Integer status;

    private Integer qps;

    private OffsetDateTime createdAt;

    @TableLogic
    private Boolean deleted;
}
