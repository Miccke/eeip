package com.eeip.module.integration.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 生产方系统：平台接入的"谁"。
 *
 * <p>一个系统下可以有多个连接器（怎么连）与多个接口（能做什么）。</p>
 */
@Data
@TableName("t_system")
public class ProviderSystem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 系统编码，同环境下唯一 */
    private String code;

    private String name;

    /** 环境：DEV / TEST / PROD */
    private String env;

    /** 主协议 */
    private String protocol;

    private String baseUrl;

    private String owner;

    private String ownerPhone;

    private Integer status;

    private String remark;

    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;

    @TableLogic
    private Boolean deleted;

    private Integer versionLock;
}
