package com.eeip.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eeip.common.api.PageResult;
import com.eeip.common.tenant.TenantContext;
import com.eeip.module.system.entity.SysAuditLog;
import com.eeip.module.system.mapper.SysAuditLogMapper;
import com.eeip.module.system.query.AuditLogQuery;
import com.eeip.module.system.vo.AuditActionVO;
import com.eeip.module.system.vo.AuditLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 审计日志查询服务。
 */
@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final SysAuditLogMapper auditLogMapper;

    /**
     * 分页查询审计日志。
     */
    public PageResult<AuditLogVO> page(AuditLogQuery q) {
        LambdaQueryWrapper<SysAuditLog> w = Wrappers.<SysAuditLog>lambdaQuery()
                // 审计表不参与多租户拦截器（避免异步入库丢失上下文），这里显式限定租户
                .eq(SysAuditLog::getTenantId, TenantContext.getOrDefault())
                .and(q.operator() != null && !q.operator().isBlank(),
                        x -> x.like(SysAuditLog::getOperatorId, q.operator())
                                .or().like(SysAuditLog::getOperatorName, q.operator()))
                .eq(q.action() != null && !q.action().isBlank(),
                        SysAuditLog::getAction, q.action())
                .eq(q.result() != null && !q.result().isBlank(),
                        SysAuditLog::getResult, q.result())
                .eq(q.resourceType() != null && !q.resourceType().isBlank(),
                        SysAuditLog::getResourceType, q.resourceType())
                .ge(q.startTime() != null && !q.startTime().isBlank(),
                        SysAuditLog::getOccurredAt, parseTime(q.startTime()))
                .le(q.endTime() != null && !q.endTime().isBlank(),
                        SysAuditLog::getOccurredAt, parseTime(q.endTime()))
                .orderByDesc(SysAuditLog::getOccurredAt);

        Page<SysAuditLog> page = auditLogMapper.selectPage(
                new Page<>(q.safePageNum(), q.safePageSize()), w);

        return PageResult.of(page, this::toVO);
    }

    /**
     * 返回出现过的动作清单，供前端筛选下拉使用。
     */
    public List<AuditActionVO> actions() {
        return auditLogMapper.selectList(Wrappers.<SysAuditLog>lambdaQuery()
                        .select(SysAuditLog::getAction, SysAuditLog::getActionName)
                        .groupBy(SysAuditLog::getAction, SysAuditLog::getActionName))
                .stream()
                .map(l -> new AuditActionVO(l.getAction(), l.getActionName()))
                .filter(a -> Objects.nonNull(a.code()))
                .toList();
    }

    /**
     * 解析时间参数，兼容 ISO 8601 与 "yyyy-MM-dd HH:mm:ss"；为空时返回 null。
     */
    private OffsetDateTime parseTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text);
        } catch (Exception e) {
            // 兼容前端传来的 "yyyy-MM-dd HH:mm:ss" 格式
            return java.time.LocalDateTime.parse(text.replace(' ', 'T'))
                    .atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime();
        }
    }

    private AuditLogVO toVO(SysAuditLog e) {
        String endpoint = (e.getRequestMethod() == null ? "" : e.getRequestMethod() + " ")
                + (e.getRequestUri() == null ? "" : e.getRequestUri());
        return new AuditLogVO(e.getId(), e.getAction(), e.getActionName(), e.getResourceType(),
                e.getResourceId(), e.getOperatorId(), e.getOperatorName(), endpoint.trim(),
                e.getRequestParams(), e.getResult(), e.getErrorMsg(), e.getCostMs(),
                e.getClientIp(), e.getOccurredAt());
    }
}
