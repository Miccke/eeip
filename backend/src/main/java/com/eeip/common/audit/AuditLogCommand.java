package com.eeip.common.audit;

import lombok.Data;

/**
 * 审计写入命令（切面 → 服务 的内部传输对象）。
 */
@Data
public class AuditLogCommand {
    private String operatorId;
    private String operatorName;
    private String action;
    private String actionName;
    private String resourceType;
    private String resourceId;
    private String requestMethod;
    private String requestUri;
    private String requestParams;
    private String result;
    private String errorMsg;
    private Integer costMs;
    private String clientIp;
    private String userAgent;
}
