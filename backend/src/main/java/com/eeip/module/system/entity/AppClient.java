package com.eeip.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 接入应用（调用方）：A 系统注册后获得 appKey / appSecret。
 */
@Data
@TableName("t_app_client")
public class AppClient {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 应用标识 */
    private String appKey;

    /** 签名密钥（演示阶段明文） */
    private String appSecret;

    private String appName;

    private String owner;

    /** 1 启用 0 停用 */
    private Integer status;

    private OffsetDateTime expireAt;

    /** 允许的来源 IP，逗号分隔，空表示不限 */
    private String allowedIps;

    private String remark;

    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;

    @TableLogic
    private Boolean deleted;

    private Integer versionLock;
}
