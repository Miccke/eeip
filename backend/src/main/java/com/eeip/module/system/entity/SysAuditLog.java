package com.eeip.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 操作审计日志实体（对应 sys_audit_log，只追加，不修改）。
 */
@Data
@TableName("sys_audit_log")
public class SysAuditLog {

    /** 主键（雪花 ID） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户 ID */
    private Long tenantId;

    /** 动作编码，如 USER_CREATE */
    private String action;

    /** 动作中文名 */
    private String actionName;

    /** 资源类型，如 USER / ROLE */
    private String resourceType;

    /** 资源标识（用户 ID、角色 ID 等） */
    private String resourceId;

    /** 操作人 ID */
    private String operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** HTTP 方法 */
    private String requestMethod;

    /** 请求路径 */
    private String requestUri;

    /** 请求参数 JSON（已脱敏） */
    private String requestParams;

    /** 结果：SUCCESS / FAILED */
    private String result;

    /** 失败原因 */
    private String errorMsg;

    /** 耗时（毫秒） */
    private Integer costMs;

    /** 客户端 IP */
    private String clientIp;

    /** User-Agent */
    private String userAgent;

    /** 发生时间 */
    private OffsetDateTime occurredAt;
}
