package com.eeip.module.integration.controller;

import com.eeip.common.api.R;
import com.eeip.common.audit.AuditLog;
import com.eeip.module.integration.entity.IntFlowTrigger;
import com.eeip.module.integration.service.FlowTriggerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 触发计划：查看与启停由流程 DSL 生成的触发器。
 */
@Tag(name = "集成 · 触发计划", description = "定时/MQ/事件触发器，由发布版本的 DSL 生成")
@RestController
@RequestMapping("/api/v1/integration/flows/{flowId}/triggers")
@RequiredArgsConstructor
public class FlowTriggerController {

    private final FlowTriggerService service;

    @Operation(summary = "触发计划列表", description = "含下次触发时间、最近一次结果与触发次数。")
    @GetMapping
    @PreAuthorize("hasAuthority('asset:flow:trigger')")
    public R<List<IntFlowTrigger>> list(@Parameter(description = "流程 ID") @PathVariable Long flowId) {
        return R.ok(service.listByFlow(flowId));
    }

    @Operation(summary = "启用/暂停触发器")
    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('asset:flow:trigger')")
    @AuditLog(action = "FLOW_TRIGGER_TOGGLE", actionName = "切换触发计划",
            resourceType = "FLOW_TRIGGER", resourceId = "#id")
    public R<Void> toggle(@PathVariable Long flowId,
                          @PathVariable Long id,
                          @Parameter(description = "true 启用 / false 暂停") @RequestParam boolean active) {
        service.toggle(id, active);
        return R.ok();
    }
}
