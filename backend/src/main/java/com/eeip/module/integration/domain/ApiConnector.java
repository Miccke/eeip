package com.eeip.module.integration.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 接口版本与连接器的绑定：一个接口版本可挂多个连接器（主/备）。
 */
@Data
@TableName("t_api_connector")
public class ApiConnector {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long apiVersionId;

    private String connectorCode;

    /** MAIN / BACKUP */
    private String role;

    private Integer weight;

    private Integer status;

    private OffsetDateTime createdAt;

    @TableLogic
    private Boolean deleted;
}
