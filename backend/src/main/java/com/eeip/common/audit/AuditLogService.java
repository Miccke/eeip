package com.eeip.common.audit;

import com.eeip.common.security.LoginUser;
import com.eeip.common.security.SecurityUtils;
import com.eeip.module.system.entity.SysAuditLog;
import com.eeip.module.system.mapper.SysAuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * 审计日志写入服务。
 *
 * <p>使用独立线程池异步写入（见 {@code AuditAsyncConfig}），
 * 保证审计 IO 不进入业务响应时间；线程池满时丢弃并告警，不阻塞业务。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final SysAuditLogMapper auditLogMapper;

    @Async("auditExecutor")
    public void saveAsync(AuditLogCommand cmd) {
        try {
            LoginUser current = SecurityUtils.currentUser();
            SysAuditLog log = new SysAuditLog();
            log.setTenantId(current == null ? 1L : current.tenantId());
            log.setOperatorId(cmd.getOperatorId());
            log.setOperatorName(cmd.getOperatorName());
            log.setAction(cmd.getAction());
            log.setActionName(cmd.getActionName());
            log.setResourceType(cmd.getResourceType());
            log.setResourceId(cmd.getResourceId());
            log.setRequestMethod(cmd.getRequestMethod());
            log.setRequestUri(cmd.getRequestUri());
            log.setRequestParams(cmd.getRequestParams());
            log.setResult(cmd.getResult());
            log.setErrorMsg(cmd.getErrorMsg());
            log.setCostMs(cmd.getCostMs());
            log.setClientIp(cmd.getClientIp());
            log.setUserAgent(cmd.getUserAgent());
            log.setOccurredAt(OffsetDateTime.now());
            auditLogMapper.insert(log);
        } catch (Exception e) {
            log.warn("审计落库失败 action={} operator={}", cmd.getAction(), cmd.getOperatorId(), e);
        }
    }
}
