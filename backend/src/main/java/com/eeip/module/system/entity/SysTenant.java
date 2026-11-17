package com.eeip.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 租户实体（平台级表，不参与租户过滤）。
 */
@Data
@TableName("sys_tenant")
public class SysTenant {

    /** 租户 ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户编码（登录时用于识别） */
    private String code;

    /** 租户名称 */
    private String name;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    /** 类型：INTERNAL 内部 / PARTNER 合作方 */
    private String tenantType;

    /** 联系人 */
    private String contactName;

    /** 联系电话 */
    private String contactPhone;

    /** 到期时间 */
    private OffsetDateTime expireAt;

    /** 备注 */
    private String remark;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    @TableLogic
    private Boolean deleted;

    @Version
    private Integer version;
}
