package com.eeip.module.integration.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eeip.common.api.ErrorCode;
import com.eeip.common.api.PageResult;
import com.eeip.common.api.R;
import com.eeip.common.audit.AuditLog;
import com.eeip.common.exception.BizException;
import com.eeip.common.security.SecurityUtils;
import com.eeip.module.integration.engine.FlowEngine;
import com.eeip.module.integration.entity.IntFlow;
import com.eeip.module.integration.entity.IntFlowDeadLetter;
import com.eeip.module.integration.mapper.IntFlowMapper;
import com.eeip.module.integration.service.DeadLetterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 死信处置：查看失败实例、重投、丢弃。
 */
@Slf4j
@Tag(name = "集成 · 死信处置", description = "失败实例的重投与丢弃")
@RestController
@RequestMapping("/api/v1/integration/dead-letters")
@RequiredArgsConstructor
public class DeadLetterController {

    private final DeadLetterService service;
    private final FlowEngine engine;
    private final IntFlowMapper flowMapper;
    private final ObjectMapper json;

    @Operation(summary = "死信列表")
    @GetMapping
    @PreAuthorize("hasAuthority('ops:dlq:list')")
    public R<PageResult<IntFlowDeadLetter>> list(
            @Parameter(description = "状态：PENDING / RETRIED / DISCARDED") @RequestParam(required = false) String status,
            @Parameter(description = "流程编码") @RequestParam(required = false) String flowCode,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        return R.ok(service.list(status, flowCode, pageNum, pageSize));
    }

    @Operation(summary = "死信统计")
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('ops:dlq:list')")
    public R<Map<String, Long>> stats() {
        return R.ok(service.stats());
    }

    @Operation(summary = "死信详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ops:dlq:list')")
    public R<IntFlowDeadLetter> detail(@PathVariable Long id) {
        IntFlowDeadLetter d = service.byId(id);
        if (d == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "死信不存在");
        }
        return R.ok(d);
    }

    @Operation(summary = "重投", description = "用原始入参重新执行当前发布版本，生成新实例。")
    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAuthority('ops:dlq:retry')")
    @AuditLog(action = "DLQ_RETRY", actionName = "死信重投",
            resourceType = "DEAD_LETTER", resourceId = "#id")
    public R<FlowEngine.ExecutionResult> retry(@PathVariable Long id) {
        IntFlowDeadLetter d = service.byId(id);
        if (d == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "死信不存在");
        }
        if ("RETRIED".equals(d.getStatus()) || "DISCARDED".equals(d.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "该死信已处理，不能重复重投");
        }
        IntFlow flow = flowMapper.selectById(d.getFlowId());
        if (flow == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "流程已删除，无法重投");
        }
        Map<String, Object> input = parse(d.getInputJson());
        FlowEngine.ExecutionResult r = engine.run(flow, input, "RETRY");
        service.markRetried(id, Long.valueOf(r.instanceId()), SecurityUtils.currentUsername());
        log.info("死信 {} 重投完成，新实例 {} 状态 {}", id, r.instanceId(), r.status());
        return R.ok(r);
    }

    @Operation(summary = "丢弃", description = "标记为不再处理（保留记录）。")
    @PostMapping("/{id}/discard")
    @PreAuthorize("hasAuthority('ops:dlq:retry')")
    @AuditLog(action = "DLQ_DISCARD", actionName = "死信丢弃",
            resourceType = "DEAD_LETTER", resourceId = "#id")
    public R<Void> discard(@PathVariable Long id,
                           @Parameter(description = "处理备注") @RequestParam(required = false) String remark) {
        service.discard(id, SecurityUtils.currentUsername(), remark);
        return R.ok();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String s) {
        try {
            return s == null || s.isBlank() ? Map.of() : json.readValue(s, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
