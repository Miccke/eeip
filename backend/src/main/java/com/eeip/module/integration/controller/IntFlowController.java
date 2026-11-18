package com.eeip.module.integration.controller;

import com.eeip.common.api.R;
import com.eeip.common.audit.AuditLog;
import com.eeip.module.integration.dto.FlowDsl;
import com.eeip.module.integration.dto.FlowSaveRequest;
import com.eeip.module.integration.entity.IntFlowVersion;
import com.eeip.module.integration.service.FlowService;
import com.eeip.module.integration.vo.FlowDetailVO;
import com.eeip.module.integration.vo.FlowVO;
import com.eeip.module.integration.vo.FlowValidateResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程编排接口。
 */
@Tag(name = "集成 · 流程编排", description = "流程定义的增删改查、DSL 校验、发布与版本")
@RestController
@RequestMapping("/api/v1/integration/flows")
@RequiredArgsConstructor
public class IntFlowController {

    private final FlowService flowService;

    @Operation(summary = "流程列表")
    @GetMapping
    @PreAuthorize("hasAuthority('asset:flow:list')")
    public R<List<FlowVO>> list(
            @Parameter(description = "编码或名称，模糊匹配", example = "demo")
            @RequestParam(required = false) String keyword) {
        return R.ok(flowService.list(keyword));
    }

    @Operation(summary = "流程详情", description = "返回当前版本（或草稿）的完整 DSL。")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:flow:list')")
    public R<FlowDetailVO> detail(
            @Parameter(description = "流程 ID", required = true) @PathVariable Long id) {
        return R.ok(flowService.detail(id));
    }

    @Operation(summary = "新建流程", description = "同时创建一个 1.0.0 草稿版本。")
    @PostMapping
    @PreAuthorize("hasAuthority('asset:flow:create')")
    @AuditLog(action = "FLOW_CREATE", actionName = "新建流程",
            resourceType = "FLOW", resourceId = "#request.code()")
    public R<Long> create(@RequestBody @Valid FlowSaveRequest request) {
        return R.ok(flowService.create(request));
    }

    @Operation(summary = "编辑流程", description = "更新元数据与草稿 DSL，不影响已发布版本。")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:flow:update')")
    @AuditLog(action = "FLOW_UPDATE", actionName = "编辑流程",
            resourceType = "FLOW", resourceId = "#id")
    public R<Void> update(
            @Parameter(description = "流程 ID", required = true) @PathVariable Long id,
            @RequestBody @Valid FlowSaveRequest request) {
        flowService.update(id, request);
        return R.ok();
    }

    @Operation(summary = "删除流程", description = "已发布的流程需先下线。")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:flow:delete')")
    @AuditLog(action = "FLOW_DELETE", actionName = "删除流程",
            resourceType = "FLOW", resourceId = "#id")
    public R<Void> delete(
            @Parameter(description = "流程 ID", required = true) @PathVariable Long id) {
        flowService.delete(id);
        return R.ok();
    }

    @Operation(summary = "校验 DSL", description = "不落库，返回错误与告警。结构、引用、可达性、环检测、节点配置都会检查。")
    @PostMapping("/validate")
    @PreAuthorize("hasAuthority('asset:flow:validate')")
    public R<FlowValidateResult> validate(@RequestBody @Valid FlowDsl dsl) {
        return R.ok(flowService.validate(dsl));
    }

    @Operation(summary = "发布流程", description = "校验通过后生成新版本，旧版本置为 DEPRECATED。")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('asset:flow:publish')")
    @AuditLog(action = "FLOW_PUBLISH", actionName = "发布流程",
            resourceType = "FLOW", resourceId = "#id")
    public R<FlowVO> publish(
            @Parameter(description = "流程 ID", required = true) @PathVariable Long id,
            @Parameter(description = "变更说明", example = "首个版本") @RequestParam(required = false) String changeLog) {
        return R.ok(flowService.publish(id, changeLog));
    }

    @Operation(summary = "下线流程")
    @PostMapping("/{id}/offline")
    @PreAuthorize("hasAuthority('asset:flow:publish')")
    @AuditLog(action = "FLOW_OFFLINE", actionName = "下线流程",
            resourceType = "FLOW", resourceId = "#id")
    public R<Void> offline(
            @Parameter(description = "流程 ID", required = true) @PathVariable Long id) {
        flowService.offline(id);
        return R.ok();
    }

    @Operation(summary = "版本历史")
    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAuthority('asset:flow:list')")
    public R<List<IntFlowVersion>> versions(
            @Parameter(description = "流程 ID", required = true) @PathVariable Long id) {
        return R.ok(flowService.versions(id));
    }
}
